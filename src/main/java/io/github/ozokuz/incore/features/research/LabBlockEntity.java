package io.github.ozokuz.incore.features.research;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LabBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int INPUT_SLOT_COUNT = 9;
    public static final int FUEL_SLOT = 9;
    public static final int MODULE_START_SLOT = 10;
    public static final int MODULE_SLOT_COUNT = 5;
    public static final int TOTAL_SLOT_COUNT = 15;

    public static final int STATUS_NO_RESEARCH_SELECTED = 0;
    public static final int STATUS_NOT_ENOUGH_MATERIALS = 1;
    public static final int STATUS_WORKING = 2;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_ACTIVE_RESEARCH_INDEX = 2;
    private static final int DATA_STATUS = 3;
    private static final int DATA_OVERALL_PROGRESS = 4;
    private static final int DATA_OVERALL_MAX = 5;
    private static final int DATA_TIER = 6;
    private static final int DATA_BURN_TIME = 7;
    private static final int DATA_BURN_TOTAL = 8;
    private static final int DATA_ENERGY = 9;
    private static final int DATA_ENERGY_CAP = 10;
    private static final int DATA_MECHANICAL_RPM = 11;
    private static final int DATA_MECHANICAL_STRESS = 12;
    private static final int DATA_MODULAR_SPEED_BONUS = 13;
    private static final int DATA_MODULAR_PRODUCTIVITY_BONUS = 14;
    private static final int DATA_COUNT = 15;

    private final NonNullList<ItemStack> slots = NonNullList.withSize(TOTAL_SLOT_COUNT, ItemStack.EMPTY);
    private @Nullable UUID owner;
    private String ownerScopeKey = "";
    private String ownerDisplayName = "";
    private int progress;
    private float progressRemainder;
    private int maxProgress;
    private @Nullable ResourceLocation activeResearchId;
    private int activeResearchSyncIndex = -1;
    private int labStatus = STATUS_NO_RESEARCH_SELECTED;
    private int overallProgressSync;
    private int overallMaxProgressSync;
    private int burnTime;
    private int burnTimeTotal;
    private int animationTick;
    private float mechanicalRpm;
    private float mechanicalStress;
    private int modularSpeedBonusSync;
    private int modularProductivityBonusSync;

    private final LabEnergyStorage modularEnergyStorage = new LabEnergyStorage();

    public LabBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.LAB_BLOCK_ENTITY.get(), pos, blockState);
    }

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> Math.max(0, progress);
                case DATA_MAX_PROGRESS -> Math.max(0, maxProgress);
                case DATA_ACTIVE_RESEARCH_INDEX -> {
                    if (level != null && !level.isClientSide) {
                        syncResearchIndex();
                    }
                    yield activeResearchSyncIndex;
                }
                case DATA_STATUS -> labStatus;
                case DATA_OVERALL_PROGRESS -> {
                    if (level != null && !level.isClientSide) {
                        yield overallProgressFromQueue();
                    }
                    yield Math.max(0, overallProgressSync);
                }
                case DATA_OVERALL_MAX -> {
                    if (level != null && !level.isClientSide) {
                        yield overallMaxFromQueue();
                    }
                    yield Math.max(0, overallMaxProgressSync);
                }
                case DATA_TIER -> labTier().ordinal();
                case DATA_BURN_TIME -> Math.max(0, burnTime);
                case DATA_BURN_TOTAL -> Math.max(0, burnTimeTotal);
                case DATA_ENERGY -> modularEnergyStorage.getEnergyStored();
                case DATA_ENERGY_CAP -> modularEnergyStorage.getMaxEnergyStored();
                case DATA_MECHANICAL_RPM -> Math.max(0, Math.round(mechanicalRpm));
                case DATA_MECHANICAL_STRESS -> Math.max(0, Math.round(mechanicalStress));
                case DATA_MODULAR_SPEED_BONUS -> {
                    if (level != null && !level.isClientSide) {
                        yield encodeBonusBasisPoints(modularSpeedBonus());
                    }
                    yield Math.max(0, modularSpeedBonusSync);
                }
                case DATA_MODULAR_PRODUCTIVITY_BONUS -> {
                    if (level != null && !level.isClientSide) {
                        yield encodeBonusBasisPoints(modularProductivityBonus());
                    }
                    yield Math.max(0, modularProductivityBonusSync);
                }
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case DATA_PROGRESS -> progress = Math.max(0, value);
                case DATA_MAX_PROGRESS -> maxProgress = Math.max(0, value);
                case DATA_ACTIVE_RESEARCH_INDEX -> {
                    activeResearchSyncIndex = value;
                    activeResearchId = decodeResearchIndex(value);
                    if (activeResearchSyncIndex < 0) {
                        progress = 0;
                        maxProgress = 0;
                        labStatus = STATUS_NO_RESEARCH_SELECTED;
                    }
                }
                case DATA_STATUS -> labStatus = Math.clamp(value, STATUS_NO_RESEARCH_SELECTED, STATUS_WORKING);
                case DATA_OVERALL_PROGRESS -> overallProgressSync = Math.max(0, value);
                case DATA_OVERALL_MAX -> overallMaxProgressSync = Math.max(0, value);
                case DATA_BURN_TIME -> burnTime = Math.max(0, value);
                case DATA_BURN_TOTAL -> burnTimeTotal = Math.max(0, value);
                case DATA_ENERGY -> modularEnergyStorage.setStoredEnergyRaw(value);
                case DATA_MECHANICAL_RPM -> mechanicalRpm = Math.max(0, value);
                case DATA_MECHANICAL_STRESS -> mechanicalStress = Math.max(0, value);
                case DATA_MODULAR_SPEED_BONUS -> modularSpeedBonusSync = Math.max(0, value);
                case DATA_MODULAR_PRODUCTIVITY_BONUS -> modularProductivityBonusSync = Math.max(0, value);
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public static void tick(Level level, BlockPos pos, BlockState state, LabBlockEntity lab) {
        if (level.isClientSide) {
            return;
        }
        if (level.getServer() == null) {
            return;
        }

        ServerPlayer player = lab.resolveProgressPlayer();
        if (player == null) {
            return;
        }

        lab.serverTick(player);
    }

    private void serverTick(ServerPlayer player) {
        animationTick++;
        if (activeResearchId == null || !matchesActiveResearch(player)) {
            refreshActiveProcess(player, true);
        }

        if (activeResearchId == null || maxProgress <= 0) {
            updateLabStatus();
            setChanged();
            return;
        }

        ResearchEntryData entry = ResearchEntryManager.all().get(activeResearchId);
        if (entry == null) {
            refreshActiveProcess(player, true);
            setChanged();
            return;
        }

        if (!hasRequiredMaterials(entry)) {
            progress = 0;
            progressRemainder = 0;
            updateLabStatus();
            setChanged();
            return;
        }

        if (!consumePowerForTick()) {
            progress = 0;
            progressRemainder = 0;
            updateLabStatus();
            setChanged();
            return;
        }

        float speedMultiplier = processingSpeedMultiplier();
        progressRemainder += Math.max(0.05F, speedMultiplier);
        while (progressRemainder >= 1.0F) {
            progress++;
            progressRemainder -= 1.0F;
        }

        if (progress >= maxProgress) {
            if (!consumeRequiredMaterials(entry)) {
                refreshActiveProcess(player, true);
                setChanged();
                return;
            }

            int progressGain = 1;
            if (labTier() == LabTier.MODULAR && level != null) {
                float productivity = modularProductivityBonus();
                if (productivity > 0 && level.random.nextFloat() < productivity) {
                    progressGain++;
                }
            }

            ResearchProgressService.addResearchProgress(player, activeResearchId, progressGain);
            refreshActiveProcess(player, true);
            emitMaterialParticles(entry);
        }

        updateLabStatus();
        setChanged();
    }

    private void emitMaterialParticles(ResearchEntryData entry) {
        if (!(level instanceof ServerLevel serverLevel) || animationTick % 6 != 0) {
            return;
        }

        List<Integer> colors = activeMaterialColors(entry);
        if (colors.isEmpty()) {
            return;
        }

        int color = colors.get(Math.floorMod(animationTick / 6, colors.size()));
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 1.1D;
        double z = worldPosition.getZ() + 0.5D;

        serverLevel.sendParticles(
                new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(r, g, b), 1.2F),
                x,
                y,
                z,
                4,
                0.2D,
                0.08D,
                0.2D,
                0.01D
        );
    }

    private boolean consumePowerForTick() {
        return switch (labTier()) {
            case BURNER -> consumeBurnerPower();
            case MECHANICAL -> consumeMechanicalPower();
            case MODULAR -> consumeModularPower();
        };
    }

    private boolean consumeBurnerPower() {
        if (burnTime > 0) {
            burnTime--;
            return true;
        }

        ItemStack fuel = slots.get(FUEL_SLOT);
        if (fuel.isEmpty()) {
            return false;
        }
        int fuelTime = fuel.getBurnTime(null);
        if (fuelTime <= 0) {
            return false;
        }

        fuel.shrink(1);
        if (fuel.isEmpty()) {
            slots.set(FUEL_SLOT, ItemStack.EMPTY);
        }
        burnTime = fuelTime;
        burnTimeTotal = fuelTime;
        return true;
    }

    private boolean consumeMechanicalPower() {
        float rpm = resolveMechanicalRpm();
        mechanicalRpm = rpm;
        mechanicalStress = Config.MECHANICAL_LAB_STRESS_PER_RPM.get() * rpm;
        return rpm > 0.5F;
    }

    private boolean consumeModularPower() {
        int perTick = modularEnergyPerTick();
        if (perTick <= 0) {
            return true;
        }
        if (modularEnergyStorage.getEnergyStored() < perTick) {
            return false;
        }
        modularEnergyStorage.extractEnergy(perTick, false);
        return true;
    }

    private int modularEnergyPerTick() {
        if (labTier() != LabTier.MODULAR) {
            return 0;
        }
        float speedBonus = modularSpeedBonus();
        return Math.max(1, Math.round(Config.MODULAR_LAB_FE_PER_TICK.get() * (1.0F + speedBonus)));
    }

    private float processingSpeedMultiplier() {
        return switch (labTier()) {
            case BURNER -> Config.BURNER_LAB_SPEED_MULTIPLIER.get().floatValue();
            case MECHANICAL -> Math.max(0.25F, (resolveMechanicalRpm() / 32.0F) * Config.MECHANICAL_LAB_SPEED_PER_32_RPM.get().floatValue());
            case MODULAR -> 1.0F + modularSpeedBonus();
        };
    }

    private static int encodeBonusBasisPoints(float bonus) {
        return Math.max(0, Math.round(Math.max(0.0F, bonus) * 10_000.0F));
    }

    private float modularSpeedBonus() {
        float bonus = 0.0F;
        for (int slot = MODULE_START_SLOT; slot < MODULE_START_SLOT + MODULE_SLOT_COUNT; slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.is(Registration.SPEED_MODULE_CARD_ITEM.get())) {
                bonus += Config.MODULAR_LAB_SPEED_CARD_BONUS.get().floatValue();
            }
        }
        return Math.min(Config.MODULAR_LAB_MAX_SPEED_BONUS.get().floatValue(), bonus);
    }

    private float modularProductivityBonus() {
        float bonus = 0.0F;
        for (int slot = MODULE_START_SLOT; slot < MODULE_START_SLOT + MODULE_SLOT_COUNT; slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.is(Registration.PRODUCTIVITY_MODULE_CARD_ITEM.get())) {
                bonus += Config.MODULAR_LAB_PRODUCTIVITY_CARD_BONUS.get().floatValue();
            }
        }
        return Math.min(Config.MODULAR_LAB_MAX_PRODUCTIVITY_BONUS.get().floatValue(), bonus);
    }

    private float resolveMechanicalRpm() {
        if (level == null) {
            return 0.0F;
        }
        float maxRpm = 0.0F;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(direction);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be == null) {
                continue;
            }
            try {
                Method method = be.getClass().getMethod("getSpeed");
                Object speedRaw = method.invoke(be);
                if (speedRaw instanceof Number speedNumber) {
                    maxRpm = Math.max(maxRpm, Math.abs(speedNumber.floatValue()));
                }
            } catch (Throwable ignored) {
            }
        }
        return maxRpm;
    }

    private boolean matchesActiveResearch(ServerPlayer player) {
        if (activeResearchId == null) {
            return false;
        }

        ResourceLocation queueActive = ResearchProgressService.activeResearch(player);
        if (queueActive == null || !queueActive.equals(activeResearchId)) {
            return false;
        }

        ResearchEntryData entry = ResearchEntryManager.all().get(activeResearchId);
        return entry != null && !entry.researchMaterials().isEmpty();
    }

    private void refreshActiveProcess(ServerPlayer player, boolean resetProgress) {
        ResourceLocation previousResearch = activeResearchId;
        maxProgress = 0;
        activeResearchId = null;

        ResourceLocation activeResearch = ResearchProgressService.activeResearch(player);
        if (activeResearch != null) {
            ResearchEntryData entry = ResearchEntryManager.all().get(activeResearch);
            if (entry != null && !entry.researchMaterials().isEmpty()) {
                activeResearchId = activeResearch;
                maxProgress = entry.runDurationTicks();
                if (resetProgress || previousResearch == null || !previousResearch.equals(activeResearchId)) {
                    progress = 0;
                    progressRemainder = 0;
                } else {
                    progress = Math.clamp(progress, 0, maxProgress);
                }
                syncResearchIndex();
                updateLabStatus();
                return;
            }
        }

        progress = 0;
        progressRemainder = 0;
        syncResearchIndex();
        updateLabStatus();
    }

    private boolean hasRequiredMaterials(ResearchEntryData entry) {
        Map<Item, Integer> required = requiredByItem(entry);
        if (required.isEmpty()) {
            return false;
        }

        for (Map.Entry<Item, Integer> requirement : required.entrySet()) {
            int available = 0;
            for (int slot = 0; slot < INPUT_SLOT_COUNT; slot++) {
                ItemStack stack = slots.get(slot);
                if (!stack.isEmpty() && stack.is(requirement.getKey())) {
                    available += stack.getCount();
                }
            }
            if (available < requirement.getValue()) {
                return false;
            }
        }

        return true;
    }

    private boolean consumeRequiredMaterials(ResearchEntryData entry) {
        Map<Item, Integer> required = requiredByItem(entry);
        if (required.isEmpty() || !hasRequiredMaterials(entry)) {
            return false;
        }

        for (Map.Entry<Item, Integer> requirement : required.entrySet()) {
            int remaining = requirement.getValue();
            for (int slot = 0; slot < INPUT_SLOT_COUNT && remaining > 0; slot++) {
                ItemStack stack = slots.get(slot);
                if (stack.isEmpty() || !stack.is(requirement.getKey())) {
                    continue;
                }
                int removed = Math.min(remaining, stack.getCount());
                stack.shrink(removed);
                if (stack.isEmpty()) {
                    slots.set(slot, ItemStack.EMPTY);
                }
                remaining -= removed;
            }
        }
        return true;
    }

    private List<Integer> activeMaterialColors(ResearchEntryData entry) {
        List<Integer> colors = new ArrayList<>();
        for (ResearchMaterialData material : entry.researchMaterials()) {
            colors.add(material.color());
        }
        return colors;
    }

    private Map<Item, Integer> requiredByItem(ResearchEntryData entry) {
        Map<Item, Integer> required = new HashMap<>();
        for (ResearchMaterialData material : entry.researchMaterials()) {
            if (material.itemId() == null || material.itemCount() <= 0) {
                continue;
            }
            Item item = BuiltInRegistries.ITEM.get(material.itemId());
            if (item == null) {
                continue;
            }
            required.merge(item, material.itemCount(), Integer::sum);
        }
        return required;
    }

    private void updateLabStatus() {
        if (activeResearchId == null || maxProgress <= 0) {
            labStatus = STATUS_NO_RESEARCH_SELECTED;
            return;
        }
        ResearchEntryData entry = ResearchEntryManager.all().get(activeResearchId);
        if (entry == null || entry.researchMaterials().isEmpty()) {
            labStatus = STATUS_NO_RESEARCH_SELECTED;
            return;
        }
        labStatus = hasRequiredMaterials(entry) ? STATUS_WORKING : STATUS_NOT_ENOUGH_MATERIALS;
    }

    private int overallProgressFromQueue() {
        ServerPlayer player = ownerPlayer();
        if (player == null) {
            return 0;
        }
        ResourceLocation queueActive = ResearchProgressService.activeResearch(player);
        if (queueActive == null) {
            return 0;
        }
        return Math.max(0, ResearchProgressService.progressFor(player, queueActive));
    }

    private int overallMaxFromQueue() {
        ServerPlayer player = ownerPlayer();
        if (player == null) {
            return 0;
        }
        ResourceLocation queueActive = ResearchProgressService.activeResearch(player);
        if (queueActive == null) {
            return 0;
        }
        ResearchEntryData entry = ResearchEntryManager.all().get(queueActive);
        if (entry == null) {
            return 0;
        }
        return Math.max(0, entry.cost());
    }

    private @Nullable ServerPlayer ownerPlayer() {
        if (level == null || level.isClientSide || level.getServer() == null) {
            return null;
        }
        return resolveProgressPlayer();
    }

    private @Nullable ServerPlayer resolveProgressPlayer() {
        if (level == null || level.isClientSide || level.getServer() == null) {
            return null;
        }

        if (owner != null) {
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
            if (ownerPlayer != null) {
                ownerScopeKey = ResearchScopeResolver.ownerKey(ownerPlayer);
                ownerDisplayName = ResearchScopeResolver.ownerDisplayName(ownerPlayer);
                return ownerPlayer;
            }
        }

        if (!ownerScopeKey.isBlank() && ownerScopeKey.startsWith("team:")) {
            for (ServerPlayer candidate : level.getServer().getPlayerList().getPlayers()) {
                if (ownerScopeKey.equals(ResearchScopeResolver.ownerKey(candidate))) {
                    ownerDisplayName = ResearchScopeResolver.ownerDisplayName(candidate);
                    return candidate;
                }
            }
        }
        return null;
    }

    public ItemStack getInput(int slot) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return slots.get(slot);
    }

    public void setInput(int slot, ItemStack stack) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
            return;
        }
        slots.set(slot, stack);
        onSlotsChanged();
    }

    public ItemStack removeInput(int slot, int amount) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slots.get(slot);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            slots.set(slot, ItemStack.EMPTY);
        }
        onSlotsChanged();
        return removed;
    }

    public ItemStack removeInputNoUpdate(int slot) {
        if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slots.get(slot);
        slots.set(slot, ItemStack.EMPTY);
        onSlotsChanged();
        return stack;
    }

    public int inputSlotCount() {
        return INPUT_SLOT_COUNT;
    }

    public boolean isInputEmpty() {
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            if (!slots.get(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public NonNullList<ItemStack> getInputs() {
        NonNullList<ItemStack> out = NonNullList.withSize(INPUT_SLOT_COUNT, ItemStack.EMPTY);
        for (int i = 0; i < INPUT_SLOT_COUNT; i++) {
            out.set(i, slots.get(i));
        }
        return out;
    }

    public int slotCount() {
        return TOTAL_SLOT_COUNT;
    }

    public ItemStack getSlotItem(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        return slots.get(slot);
    }

    public void setSlotItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= TOTAL_SLOT_COUNT) {
            return;
        }
        slots.set(slot, stack);
        onSlotsChanged();
    }

    public ItemStack removeSlotItem(int slot, int amount) {
        if (slot < 0 || slot >= TOTAL_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slots.get(slot);
        ItemStack removed = stack.split(amount);
        if (stack.isEmpty()) {
            slots.set(slot, ItemStack.EMPTY);
        }
        onSlotsChanged();
        return removed;
    }

    public ItemStack removeSlotItemNoUpdate(int slot) {
        if (slot < 0 || slot >= TOTAL_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slots.get(slot);
        slots.set(slot, ItemStack.EMPTY);
        onSlotsChanged();
        return stack;
    }

    private void onSlotsChanged() {
        progress = 0;
        progressRemainder = 0;
        setChanged();
    }

    public boolean hasFuelSlot() {
        return labTier() == LabTier.BURNER;
    }

    public boolean hasModuleSlots() {
        return labTier() == LabTier.MODULAR;
    }

    public boolean isFuelSlot(int slot) {
        return slot == FUEL_SLOT;
    }

    public boolean isModuleSlot(int slot) {
        return slot >= MODULE_START_SLOT && slot < MODULE_START_SLOT + MODULE_SLOT_COUNT;
    }

    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < INPUT_SLOT_COUNT) {
            return true;
        }
        if (slot == FUEL_SLOT) {
            return hasFuelSlot() && stack.getBurnTime(null) > 0;
        }
        if (isModuleSlot(slot)) {
            return hasModuleSlots() && (stack.is(Registration.SPEED_MODULE_CARD_ITEM.get()) || stack.is(Registration.PRODUCTIVITY_MODULE_CARD_ITEM.get()));
        }
        return false;
    }

    public LabTier labTier() {
        Block block = getBlockState().getBlock();
        if (block instanceof LabTierProvider provider) {
            return provider.incore$getLabTier();
        }
        return LabTier.BURNER;
    }

    public String labTierId() {
        return labTier().id();
    }

    public @Nullable UUID getOwner() {
        return owner;
    }

    public void setOwner(@NotNull ServerPlayer player) {
        this.owner = player.getUUID();
        this.ownerScopeKey = ResearchScopeResolver.ownerKey(player);
        this.ownerDisplayName = ResearchScopeResolver.ownerDisplayName(player);
        setChanged();
    }

    public void setOwner(@Nullable UUID owner) {
        this.owner = owner;
        if (owner == null) {
            this.ownerScopeKey = "";
            this.ownerDisplayName = "";
        }
        setChanged();
    }

    public int labStatusForDisplay() {
        return labStatus;
    }

    public String ownerScopeForDisplay() {
        if (ownerScopeKey != null && !ownerScopeKey.isBlank()) {
            return ownerScopeKey;
        }
        return owner == null ? "none" : "player:" + owner;
    }

    public String ownerNameForDisplay() {
        if (ownerDisplayName != null && !ownerDisplayName.isBlank()) {
            return ownerDisplayName;
        }
        if (level != null && !level.isClientSide && level.getServer() != null && owner != null) {
            ServerPlayer ownerPlayer = level.getServer().getPlayerList().getPlayer(owner);
            if (ownerPlayer != null) {
                ownerDisplayName = ResearchScopeResolver.ownerDisplayName(ownerPlayer);
                return ownerDisplayName;
            }
            String cachedName = level.getServer().getProfileCache().get(owner)
                    .map(com.mojang.authlib.GameProfile::getName)
                    .orElse("");
            if (!cachedName.isBlank()) {
                ownerDisplayName = cachedName;
                return ownerDisplayName;
            }
        }
        if (owner != null) {
            return owner.toString();
        }
        return "none";
    }

    public int progressForDisplay() {
        return progress;
    }

    public int maxProgressForDisplay() {
        return maxProgress;
    }

    public int overallProgressForDisplay() {
        return overallProgressFromQueue();
    }

    public int overallMaxForDisplay() {
        return overallMaxFromQueue();
    }

    public int burnTimeForDisplay() {
        return burnTime;
    }

    public int burnTimeTotalForDisplay() {
        return burnTimeTotal;
    }

    public int energyStoredForDisplay() {
        return modularEnergyStorage.getEnergyStored();
    }

    public int energyCapacityForDisplay() {
        return modularEnergyStorage.getMaxEnergyStored();
    }

    public float mechanicalRpmForDisplay() {
        return mechanicalRpm;
    }

    public float mechanicalStressForDisplay() {
        return mechanicalStress;
    }

    public EnergyStorage energyStorage() {
        return modularEnergyStorage;
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);

        for (int i = 0; i < slots.size(); i++) {
            slots.set(i, ItemStack.EMPTY);
        }

        if (tag.contains("slots", Tag.TAG_LIST)) {
            ListTag list = tag.getList("slots", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag slotTag = list.getCompound(i);
                int slot = slotTag.getInt("slot");
                if (slot < 0 || slot >= slots.size()) {
                    continue;
                }
                slots.set(slot, ItemStack.parseOptional(registries, slotTag.getCompound("stack")));
            }
        } else if (tag.contains("inputs", Tag.TAG_LIST)) {
            // Legacy support from previous multi-input format.
            ListTag list = tag.getList("inputs", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag inputTag = list.getCompound(i);
                int slot = inputTag.getInt("slot");
                if (slot < 0 || slot >= INPUT_SLOT_COUNT) {
                    continue;
                }
                slots.set(slot, ItemStack.parseOptional(registries, inputTag.getCompound("stack")));
            }
        } else {
            // Legacy support from old single-input format.
            slots.set(0, ItemStack.parseOptional(registries, tag.getCompound("input")));
        }

        owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
        ownerScopeKey = tag.getString("ownerScope");
        ownerDisplayName = tag.getString("ownerName");
        progress = Math.max(0, tag.getInt("progress"));
        maxProgress = Math.max(0, tag.getInt("maxProgress"));
        burnTime = Math.max(0, tag.getInt("burnTime"));
        burnTimeTotal = Math.max(0, tag.getInt("burnTimeTotal"));
        modularEnergyStorage.setStoredEnergyRaw(tag.getInt("energyStored"));
        activeResearchId = null;
        activeResearchSyncIndex = -1;
        labStatus = STATUS_NO_RESEARCH_SELECTED;
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        ListTag list = new ListTag();
        for (int slot = 0; slot < slots.size(); slot++) {
            ItemStack stack = slots.get(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("slot", slot);
            slotTag.put("stack", stack.saveOptional(registries));
            list.add(slotTag);
        }
        tag.put("slots", list);

        if (owner != null) {
            tag.putUUID("owner", owner);
        }
        if (ownerScopeKey != null && !ownerScopeKey.isBlank()) {
            tag.putString("ownerScope", ownerScopeKey);
        }
        if (ownerDisplayName != null && !ownerDisplayName.isBlank()) {
            tag.putString("ownerName", ownerDisplayName);
        }
        tag.putInt("progress", progress);
        tag.putInt("maxProgress", maxProgress);
        tag.putInt("burnTime", burnTime);
        tag.putInt("burnTimeTotal", burnTimeTotal);
        tag.putInt("energyStored", modularEnergyStorage.getEnergyStored());
    }

    @Override
    public @NotNull net.minecraft.network.chat.Component getDisplayName() {
        return switch (labTier()) {
            case BURNER -> net.minecraft.network.chat.Component.translatable("block.incore.burner_lab");
            case MECHANICAL -> net.minecraft.network.chat.Component.translatable("block.incore.mechanical_lab");
            case MODULAR -> net.minecraft.network.chat.Component.translatable("block.incore.modular_lab");
        };
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inventory, @NotNull Player player) {
        return new LabMenu(containerId, inventory, this);
    }

    private static int encodeResearchIndex(@Nullable ResourceLocation researchId) {
        if (researchId == null) {
            return -1;
        }
        return sortedResearchIds().indexOf(researchId);
    }

    private static @Nullable ResourceLocation decodeResearchIndex(int index) {
        List<ResourceLocation> ids = sortedResearchIds();
        if (index < 0 || index >= ids.size()) {
            return null;
        }
        return ids.get(index);
    }

    private static List<ResourceLocation> sortedResearchIds() {
        return ResearchEntryManager.all().keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    private void syncResearchIndex() {
        activeResearchSyncIndex = encodeResearchIndex(activeResearchId);
    }

    private class LabEnergyStorage extends EnergyStorage {
        private LabEnergyStorage() {
            super(Config.MODULAR_LAB_FE_CAPACITY.get(), Config.MODULAR_LAB_FE_MAX_RECEIVE.get(), Config.MODULAR_LAB_FE_MAX_EXTRACT.get());
        }

        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            if (labTier() != LabTier.MODULAR) {
                return 0;
            }
            int received = super.receiveEnergy(toReceive, simulate);
            if (received > 0 && !simulate) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            if (labTier() != LabTier.MODULAR) {
                return 0;
            }
            int extracted = super.extractEnergy(toExtract, simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }

        @Override
        public boolean canExtract() {
            return labTier() == LabTier.MODULAR && super.canExtract();
        }

        @Override
        public boolean canReceive() {
            return labTier() == LabTier.MODULAR && super.canReceive();
        }

        private void setStoredEnergyRaw(int energy) {
            this.energy = Math.max(0, Math.min(energy, this.capacity));
        }
    }
}
