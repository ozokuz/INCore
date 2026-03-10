package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.research.ResearchMaterialDefinition;
import io.github.ozokuz.incore.features.research.ResearchMaterialManager;
import io.github.ozokuz.incore.features.researchv2.ResearchDeterministicRng;
import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ResearchStationRuntime {
    private ResearchStationRuntime() {
    }

    public static ResearchControllerBlockEntity resolveAssignedController(MinecraftServer server, String teamId, List<String> assignedStationIds) {
        if (server == null || teamId == null || teamId.isBlank()) {
            return null;
        }

        if (assignedStationIds != null) {
            for (String stationId : assignedStationIds) {
                if (stationId == null || stationId.isBlank()) {
                    continue;
                }
                ResearchControllerBlockEntity controller = ResearchMultiblockStationRegistry.controllerByStationId(server, teamId, stationId);
                if (controller != null && controller.isFormed()) {
                    return controller;
                }
            }
        }
        return ResearchMultiblockStationRegistry.controllersForTeam(server, teamId).stream().findFirst().orElse(null);
    }

    public static boolean hasWritableDisk(ResearchControllerBlockEntity controller) {
        ResearchDriveBlockEntity drive = resolveDrive(controller);
        return drive != null && !drive.mountedDisk().isEmpty() && StationInventoryRules.isResearchDisk(drive.mountedDisk());
    }

    public static boolean hasRequiredModules(ResearchControllerBlockEntity controller, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        LogicHousingBlockEntity housing = resolveLogicHousing(controller);
        if (housing == null) {
            return false;
        }
        Map<String, Integer> required = foldModuleRequirements(requirements);
        Map<String, Integer> available = countFreshModuleDurability(housing);
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static boolean consumeRequiredModules(ResearchControllerBlockEntity controller, ResourceLocation nodeId, int completedRuns, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        LogicHousingBlockEntity housing = resolveLogicHousing(controller);
        if (housing == null || !hasRequiredModules(controller, requirements)) {
            return false;
        }
        Map<String, Integer> remainingByTier = new LinkedHashMap<>(foldModuleRequirements(requirements));
        for (Map.Entry<String, Integer> entry : remainingByTier.entrySet()) {
            int remaining = entry.getValue();
            LogicModuleTier tier = LogicModuleTier.fromSerialized(entry.getKey());
            if (tier == null) {
                return false;
            }
            for (int slot = 0; slot < housing.activeSlotCount() && remaining > 0; slot++) {
                ItemStack stack = housing.rawItemHandler().getStackInSlot(slot);
                if (!isMatchingFreshModule(stack, tier)) {
                    continue;
                }
                int maxDamage = stack.getMaxDamage();
                int available = Math.max(0, maxDamage - stack.getDamageValue());
                if (available <= 0) {
                    continue;
                }

                int consume = Math.min(remaining, available);
                remaining -= consume;
                int nextDamage = stack.getDamageValue() + consume;
                if (nextDamage >= maxDamage) {
                    ItemStack replacement = exhaustedReplacement(controller, nodeId, completedRuns, tier, slot);
                    housing.rawItemHandler().setStackInSlot(slot, ItemStack.EMPTY);
                    emitLogicOutput(controller, housing, replacement);
                } else {
                    stack.setDamageValue(nextDamage);
                    housing.rawItemHandler().setStackInSlot(slot, stack);
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasRequiredModules(List<ResearchControllerBlockEntity> controllers, List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        Map<String, Integer> required = foldModuleRequirements(requirements);
        if (required.isEmpty()) {
            return true;
        }

        Map<String, Integer> available = new LinkedHashMap<>();
        for (ResearchControllerBlockEntity controller : controllers) {
            LogicHousingBlockEntity housing = resolveLogicHousing(controller);
            if (housing == null) {
                continue;
            }
            countFreshModuleDurability(housing).forEach((tier, amount) -> available.merge(tier, amount, Integer::sum));
        }
        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            if (available.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static boolean consumeRequiredModules(
            List<ResearchControllerBlockEntity> controllers,
            ResearchControllerBlockEntity executor,
            ResourceLocation nodeId,
            int completedRuns,
            List<ResearchCostDefinition.LogicModuleRequirement> requirements
    ) {
        if (controllers == null || controllers.isEmpty()) {
            return false;
        }
        if (!hasRequiredModules(controllers, requirements)) {
            return false;
        }

        Map<String, Integer> remainingByTier = new LinkedHashMap<>(foldModuleRequirements(requirements));
        for (Map.Entry<String, Integer> entry : remainingByTier.entrySet()) {
            int remaining = entry.getValue();
            LogicModuleTier tier = LogicModuleTier.fromSerialized(entry.getKey());
            if (tier == null) {
                return false;
            }

            for (ResearchControllerBlockEntity controller : controllers) {
                LogicHousingBlockEntity housing = resolveLogicHousing(controller);
                if (housing == null) {
                    continue;
                }
                for (int slot = 0; slot < housing.activeSlotCount() && remaining > 0; slot++) {
                    ItemStack stack = housing.rawItemHandler().getStackInSlot(slot);
                    if (!isMatchingFreshModule(stack, tier)) {
                        continue;
                    }

                    int maxDamage = stack.getMaxDamage();
                    int available = Math.max(0, maxDamage - stack.getDamageValue());
                    if (available <= 0) {
                        continue;
                    }

                    int consume = Math.min(remaining, available);
                    remaining -= consume;
                    int nextDamage = stack.getDamageValue() + consume;
                    if (nextDamage >= maxDamage) {
                        ResearchControllerBlockEntity outputOwner = executor == null ? controller : executor;
                        ItemStack replacement = exhaustedReplacement(outputOwner, nodeId, completedRuns, tier, slot);
                        housing.rawItemHandler().setStackInSlot(slot, ItemStack.EMPTY);
                        emitLogicOutput(outputOwner, housing, replacement);
                    } else {
                        stack.setDamageValue(nextDamage);
                        housing.rawItemHandler().setStackInSlot(slot, stack);
                    }
                }
                if (remaining <= 0) {
                    break;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasRequiredMaterials(ResearchControllerBlockEntity controller, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        MaterialStorageBlockEntity storage = resolveMaterialStorage(controller);
        if (storage == null) {
            return false;
        }
        Map<String, Integer> available = ResearchStationServices.countMaterials(storage);
        for (ResearchCostDefinition.ResearchMaterialRequirement requirement : requirements) {
            if (available.getOrDefault(requirement.materialId(), 0) < Math.max(0, requirement.count())) {
                return false;
            }
        }
        return true;
    }

    public static boolean consumeRequiredMaterials(ResearchControllerBlockEntity controller, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        MaterialStorageBlockEntity storage = resolveMaterialStorage(controller);
        if (storage == null || !hasRequiredMaterials(controller, requirements)) {
            return false;
        }

        for (ResearchCostDefinition.ResearchMaterialRequirement requirement : requirements) {
            int remaining = Math.max(0, requirement.count());
            ResourceLocation materialId = ResourceLocation.tryParse(requirement.materialId());
            ResearchMaterialDefinition definition = materialId == null ? null : ResearchMaterialManager.get(materialId);
            if (definition == null) {
                return false;
            }

            for (int slot = 0; slot < storage.activeSlotCount() && remaining > 0; slot++) {
                ItemStack stack = storage.rawItemHandler().getStackInSlot(slot);
                if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(definition.itemId())) {
                    continue;
                }
                int consume = Math.min(remaining, stack.getCount());
                stack.shrink(consume);
                remaining -= consume;
                storage.rawItemHandler().setStackInSlot(slot, stack);
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public static boolean hasRequiredMaterials(List<ResearchControllerBlockEntity> controllers, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        Map<String, Integer> available = new LinkedHashMap<>();
        for (ResearchControllerBlockEntity controller : controllers) {
            MaterialStorageBlockEntity storage = resolveMaterialStorage(controller);
            if (storage == null) {
                continue;
            }
            ResearchStationServices.countMaterials(storage).forEach((materialId, amount) -> available.merge(materialId, amount, Integer::sum));
        }
        for (ResearchCostDefinition.ResearchMaterialRequirement requirement : requirements) {
            if (available.getOrDefault(requirement.materialId(), 0) < Math.max(0, requirement.count())) {
                return false;
            }
        }
        return true;
    }

    public static boolean consumeRequiredMaterials(List<ResearchControllerBlockEntity> controllers, List<ResearchCostDefinition.ResearchMaterialRequirement> requirements) {
        if (controllers == null || controllers.isEmpty()) {
            return false;
        }
        if (!hasRequiredMaterials(controllers, requirements)) {
            return false;
        }

        for (ResearchCostDefinition.ResearchMaterialRequirement requirement : requirements) {
            int remaining = Math.max(0, requirement.count());
            ResourceLocation materialId = ResourceLocation.tryParse(requirement.materialId());
            ResearchMaterialDefinition definition = materialId == null ? null : ResearchMaterialManager.get(materialId);
            if (definition == null) {
                return false;
            }

            for (ResearchControllerBlockEntity controller : controllers) {
                MaterialStorageBlockEntity storage = resolveMaterialStorage(controller);
                if (storage == null) {
                    continue;
                }
                for (int slot = 0; slot < storage.activeSlotCount() && remaining > 0; slot++) {
                    ItemStack stack = storage.rawItemHandler().getStackInSlot(slot);
                    if (stack.isEmpty() || !BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(definition.itemId())) {
                        continue;
                    }
                    int consume = Math.min(remaining, stack.getCount());
                    stack.shrink(consume);
                    remaining -= consume;
                    storage.rawItemHandler().setStackInSlot(slot, stack);
                }
                if (remaining <= 0) {
                    break;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public static void writeDiskSnapshot(ResearchControllerBlockEntity controller, ResourceLocation nodeId, int completedRuns, int requiredRuns, int lastWriteOrdinal, boolean completed, double corruptionMultiplier) {
        ResearchDriveBlockEntity drive = resolveDrive(controller);
        if (drive == null) {
            return;
        }
        ItemStack disk = drive.mountedDisk();
        if (disk.isEmpty() || !StationInventoryRules.isResearchDisk(disk)) {
            return;
        }

        List<ResearchDiskData.Snapshot> snapshots = new ArrayList<>(ResearchDiskData.readSnapshots(disk));
        if (completed) {
            snapshots.removeIf(snapshot -> snapshot.nodeId().equals(nodeId));
            ResearchDiskData.writeSnapshots(disk, snapshots);
            drive.setChanged();
            return;
        }

        Set<Integer> corruptedSegments = new LinkedHashSet<>();
        for (ResearchDiskData.Snapshot snapshot : snapshots) {
            if (snapshot.nodeId().equals(nodeId)) {
                corruptedSegments.addAll(snapshot.corruptedSegments());
            }
        }

        ResearchDiskTier diskTier = ResearchDiskData.readTier(disk);
        double chance = Math.min(1.0D, Math.max(0.0D, diskTier.corruptionChance() * Math.max(0.0D, corruptionMultiplier)));
        String eventKey = completed ? "disk_complete_" + lastWriteOrdinal : "disk_write_" + lastWriteOrdinal;
        if (ResearchDeterministicRng.rollChance(controller.teamId(), controller.stationId(), nodeId, completedRuns, eventKey, chance)) {
            int segmentCount = Math.max(1, requiredRuns);
            int segment = (int) Math.floorMod(
                    ResearchDeterministicRng.seed(controller.teamId(), controller.stationId(), nodeId, completedRuns, eventKey + "_segment"),
                    segmentCount
            );
            corruptedSegments.add(segment);
        }

        snapshots.removeIf(snapshot -> snapshot.nodeId().equals(nodeId));
        snapshots.add(new ResearchDiskData.Snapshot(nodeId, completedRuns, requiredRuns, lastWriteOrdinal, completed, Set.copyOf(corruptedSegments)));
        snapshots.sort(Comparator.comparing(snapshot -> snapshot.nodeId().toString()));
        ResearchDiskData.writeSnapshots(disk, snapshots);
        drive.setChanged();
    }

    public static boolean clearDiskCorruptedSegment(ResearchDriveBlockEntity drive, ResourceLocation nodeId, int segmentIndex) {
        if (drive == null) {
            return false;
        }
        ItemStack disk = drive.mountedDisk();
        if (disk.isEmpty() || !StationInventoryRules.isResearchDisk(disk)) {
            return false;
        }
        ResearchDiskData.clearCorruptedSegment(disk, nodeId, segmentIndex);
        drive.setChanged();
        return true;
    }

    private static Map<String, Integer> foldModuleRequirements(List<ResearchCostDefinition.LogicModuleRequirement> requirements) {
        Map<String, Integer> folded = new LinkedHashMap<>();
        for (ResearchCostDefinition.LogicModuleRequirement requirement : requirements) {
            if (requirement == null) {
                continue;
            }
            String tier = requirement.moduleTier() == null ? "" : requirement.moduleTier().strip();
            int amount = Math.max(0, requirement.durabilityCost());
            if (!tier.isBlank() && amount > 0) {
                folded.merge(tier, amount, Integer::sum);
            }
        }
        return folded;
    }

    private static Map<String, Integer> countFreshModuleDurability(LogicHousingBlockEntity housing) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (int slot = 0; slot < housing.activeSlotCount(); slot++) {
            ItemStack stack = housing.rawItemHandler().getStackInSlot(slot);
            LogicModuleTier tier = freshTier(stack);
            if (tier == null) {
                continue;
            }
            counts.merge(tier.serializedName(), Math.max(0, stack.getMaxDamage() - stack.getDamageValue()), Integer::sum);
        }
        return counts;
    }

    private static LogicModuleTier freshTier(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(Registration.BASIC_LOGIC_MODULE_ITEM.get())) {
            return LogicModuleTier.T1;
        }
        if (stack.getItem() instanceof LogicModuleItem item && item.isFresh()) {
            return item.tier();
        }
        return null;
    }

    private static boolean isMatchingFreshModule(ItemStack stack, LogicModuleTier tier) {
        return Objects.equals(freshTier(stack), tier);
    }

    private static ItemStack exhaustedReplacement(ResearchControllerBlockEntity controller, ResourceLocation nodeId, int completedRuns, LogicModuleTier tier, int slot) {
        return switch (tier) {
            case T1 -> ItemStack.EMPTY;
            case T2 -> new ItemStack(Registration.BROKEN_LOGIC_MODULE_T2_ITEM.get());
            case T3 -> ResearchDeterministicRng.rollChance(
                    controller.teamId(),
                    controller.stationId(),
                    nodeId,
                    completedRuns,
                    "logic_t3_exhaust_slot_" + slot,
                    0.50D
            ) ? new ItemStack(Registration.USED_LOGIC_MODULE_T3_ITEM.get()) : new ItemStack(Registration.BROKEN_LOGIC_MODULE_T3_ITEM.get());
            case T4 -> new ItemStack(Registration.USED_LOGIC_MODULE_T4_ITEM.get());
        };
    }

    public static LogicHousingBlockEntity resolveLogicHousing(ResearchControllerBlockEntity controller) {
        if (controller == null || controller.getLevel() == null || controller.logicHousingPos() == null) {
            return null;
        }
        return controller.getLevel().getBlockEntity(controller.logicHousingPos()) instanceof LogicHousingBlockEntity logicHousing ? logicHousing : null;
    }

    public static ResearchDriveBlockEntity resolveDrive(ResearchControllerBlockEntity controller) {
        if (controller == null || controller.getLevel() == null || controller.researchDrivePos() == null) {
            return null;
        }
        return controller.getLevel().getBlockEntity(controller.researchDrivePos()) instanceof ResearchDriveBlockEntity drive ? drive : null;
    }

    public static MaterialStorageBlockEntity resolveMaterialStorage(ResearchControllerBlockEntity controller) {
        if (controller == null || controller.getLevel() == null || controller.materialStoragePos() == null) {
            return null;
        }
        return controller.getLevel().getBlockEntity(controller.materialStoragePos()) instanceof MaterialStorageBlockEntity storage ? storage : null;
    }

    public static List<OutputPortBlockEntity> resolveOutputPorts(ResearchControllerBlockEntity controller) {
        if (controller == null || controller.getLevel() == null || controller.outputPortPositions().isEmpty()) {
            return List.of();
        }
        List<OutputPortBlockEntity> ports = new ArrayList<>();
        for (var pos : controller.outputPortPositions()) {
            if (controller.getLevel().getBlockEntity(pos) instanceof OutputPortBlockEntity outputPort) {
                ports.add(outputPort);
            }
        }
        return List.copyOf(ports);
    }

    public static OutputPortBlockEntity resolveOutputPort(ResearchControllerBlockEntity controller) {
        return resolveOutputPorts(controller).stream().findFirst().orElse(null);
    }

    private static void emitLogicOutput(ResearchControllerBlockEntity controller, LogicHousingBlockEntity housing, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        ItemStack remaining = insertIntoMatchingOutputPorts(controller, OutputPortMode.LOGIC, stack);
        if (!remaining.isEmpty() && housing != null && housing.getLevel() != null) {
            Containers.dropItemStack(housing.getLevel(), housing.getBlockPos().getX(), housing.getBlockPos().getY(), housing.getBlockPos().getZ(), remaining);
        }
    }

    public static void flushDriveOutput(ResearchControllerBlockEntity controller) {
        flushDriveOutput(controller, resolveDrive(controller));
    }

    private static void flushDriveOutput(ResearchControllerBlockEntity controller, ResearchDriveBlockEntity drive) {
        if (controller == null || drive == null) {
            return;
        }
        ItemStack disk = drive.mountedDisk();
        if (disk.isEmpty() || !ResearchDiskData.hasCorruption(disk)) {
            return;
        }
        ItemStack remaining = insertIntoMatchingOutputPorts(controller, OutputPortMode.DRIVE, disk.copy());
        if (remaining.getCount() == disk.getCount()) {
            return;
        }
        drive.rawItemHandler().setStackInSlot(0, remaining);
        drive.setChanged();
    }

    private static ItemStack insertIntoMatchingOutputPorts(ResearchControllerBlockEntity controller, OutputPortMode mode, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (OutputPortBlockEntity port : resolveOutputPorts(controller)) {
            if (port.mode() != mode) {
                continue;
            }
            remaining = port.insertOutput(remaining);
            if (remaining.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }
        return remaining;
    }

    public static AugmenterBlockEntity resolveAugmenter(ResearchControllerBlockEntity controller) {
        if (controller == null || controller.getLevel() == null || controller.augmenterPos() == null) {
            return null;
        }
        return controller.getLevel().getBlockEntity(controller.augmenterPos()) instanceof AugmenterBlockEntity augmenter ? augmenter : null;
    }
}
