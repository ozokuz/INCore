package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ResearchControllerBlockEntity extends BlockEntity {
    private static final int REVALIDATE_INTERVAL_TICKS = 20;

    private String teamId = "";
    private boolean formed;
    private String stationId = "";
    private List<BlockPos> connectedParts = List.of();
    private List<BlockPos> powerInputPositions = List.of();
    private BlockPos logicHousingPos;
    private BlockPos researchDrivePos;
    private BlockPos materialStoragePos;
    private List<BlockPos> outputPortPositions = List.of();
    private BlockPos augmenterPos;
    private ResearchPowerFamily powerFamily;
    private int powerInputTier;
    private int tickCounter;

    public ResearchControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.RESEARCH_CONTROLLER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ResearchMultiblockStationRegistry.register(this);
            revalidateStructure();
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            if (formed) {
                INCore.LOGGER.info(
                        "[ResearchV2] Station disassembled at {} in {}",
                        worldPosition,
                        level.dimension().location()
                );
            }
            ResearchMultiblockStationRegistry.unregister(this);
        }
        super.setRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResearchControllerBlockEntity controller) {
        if (level.isClientSide) {
            return;
        }
        controller.serverTick();
    }

    private void serverTick() {
        tickCounter++;
        if (tickCounter >= REVALIDATE_INTERVAL_TICKS) {
            tickCounter = 0;
            revalidateStructure();
        }
    }

    public String teamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        String normalized = teamId == null ? "" : teamId.strip();
        if (this.teamId.equals(normalized)) {
            return;
        }
        this.teamId = normalized;
        if (level != null && !level.isClientSide) {
            ResearchMultiblockStationRegistry.register(this);
        }
        setChanged();
    }

    public int stationTier() {
        if (getBlockState().getBlock() instanceof AbstractResearchControllerBlock controllerBlock) {
            return controllerBlock.tier();
        }
        return 1;
    }

    public int rpCapacity() {
        return 0;
    }

    public int slotCapacity() {
        return switch (Math.max(1, stationTier())) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 4;
        };
    }

    public int rpBuffer() {
        return 0;
    }

    public int addResearchPower(int amount) {
        return 0;
    }

    public int availableResearchPower(int amount) {
        if (!formed || level == null || powerInputPositions.isEmpty()) {
            return 0;
        }

        int remaining = Math.max(0, amount);
        int available = 0;
        for (BlockPos inputPos : powerInputPositions) {
            if (remaining <= 0) {
                break;
            }
            BlockEntity blockEntity = level.getBlockEntity(inputPos);
            if (!(blockEntity instanceof IResearchPowerInput input)) {
                continue;
            }

            int fromInput = Math.max(0, input.availableResearchPower(this, remaining));
            if (fromInput > 0) {
                available += fromInput;
                remaining -= fromInput;
            }
        }
        return available;
    }

    public int consumeResearchPower(int amount) {
        if (!formed || level == null || powerInputPositions.isEmpty()) {
            return 0;
        }

        int remaining = Math.max(0, amount);
        int consumed = 0;
        for (BlockPos inputPos : powerInputPositions) {
            if (remaining <= 0) {
                break;
            }
            BlockEntity blockEntity = level.getBlockEntity(inputPos);
            if (!(blockEntity instanceof IResearchPowerInput input)) {
                continue;
            }

            int fromInput = Math.max(0, input.pullResearchPower(this, remaining));
            if (fromInput > 0) {
                consumed += fromInput;
                remaining -= fromInput;
            }
        }
        return consumed;
    }

    public boolean isFormed() {
        return formed;
    }

    public String stationId() {
        return stationId;
    }

    public List<BlockPos> powerInputPositions() {
        return powerInputPositions;
    }

    public ResearchPowerFamily powerFamily() {
        return powerFamily;
    }

    public int powerInputTier() {
        return powerInputTier;
    }

    public List<BlockPos> connectedParts() {
        return connectedParts;
    }

    public BlockPos logicHousingPos() {
        return logicHousingPos;
    }

    public BlockPos researchDrivePos() {
        return researchDrivePos;
    }

    public BlockPos materialStoragePos() {
        return materialStoragePos;
    }

    public List<BlockPos> outputPortPositions() {
        return outputPortPositions;
    }

    public BlockPos outputPortPos() {
        return outputPortPositions.isEmpty() ? null : outputPortPositions.get(0);
    }

    public BlockPos augmenterPos() {
        return augmenterPos;
    }

    public int connectedPartCount() {
        return connectedParts.size();
    }

    public boolean revalidateStructure() {
        if (level == null || level.isClientSide) {
            return false;
        }
        ResearchMultiblockStationRegistry.register(this);

        boolean previousFormed = formed;
        ResearchStationTopology result = ResearchStationMultiblockValidator.validate(level, worldPosition);
        boolean nextFormed = result.formed();
        String nextStationId = nextFormed ? buildStationId(level.dimension().location(), worldPosition) : "";
        List<BlockPos> nextConnectedParts = nextFormed ? normalizeConnectedParts(result.connectedParts()) : List.of();
        List<BlockPos> nextPowerInputPositions = nextFormed ? normalizeConnectedParts(result.inputPositions()) : List.of();
        BlockPos nextLogicHousingPos = nextFormed ? immutablePos(result.logicHousingPos()) : null;
        BlockPos nextResearchDrivePos = nextFormed ? immutablePos(result.researchDrivePos()) : null;
        BlockPos nextMaterialStoragePos = nextFormed ? immutablePos(result.materialStoragePos()) : null;
        List<BlockPos> nextOutputPortPositions = nextFormed ? normalizeConnectedParts(result.outputPortPositions()) : List.of();
        BlockPos nextAugmenterPos = nextFormed ? immutablePos(result.augmenterPos()) : null;
        ResearchPowerFamily nextPowerFamily = nextFormed ? result.powerFamily() : null;
        int nextPowerInputTier = nextFormed ? Math.max(0, result.powerInputTier()) : 0;

        boolean changed = formed != nextFormed
                || !stationId.equals(nextStationId)
                || !connectedParts.equals(nextConnectedParts)
                || !powerInputPositions.equals(nextPowerInputPositions)
                || !java.util.Objects.equals(logicHousingPos, nextLogicHousingPos)
                || !java.util.Objects.equals(researchDrivePos, nextResearchDrivePos)
                || !java.util.Objects.equals(materialStoragePos, nextMaterialStoragePos)
                || !outputPortPositions.equals(nextOutputPortPositions)
                || !java.util.Objects.equals(augmenterPos, nextAugmenterPos)
                || powerFamily != nextPowerFamily
                || powerInputTier != nextPowerInputTier;

        clearPartBindings();
        formed = nextFormed;
        stationId = nextStationId;
        connectedParts = nextConnectedParts;
        powerInputPositions = nextPowerInputPositions;
        logicHousingPos = nextLogicHousingPos;
        researchDrivePos = nextResearchDrivePos;
        materialStoragePos = nextMaterialStoragePos;
        outputPortPositions = nextOutputPortPositions;
        augmenterPos = nextAugmenterPos;
        powerFamily = nextPowerFamily;
        powerInputTier = nextPowerInputTier;
        bindPartBindings();

        if (changed) {
            setChanged();

            if (!previousFormed && formed) {
                INCore.LOGGER.info(
                        "[ResearchV2] Station formed id={} tier={} availableRp={} parts={}",
                        stationId,
                        stationTier(),
                        availableResearchPower(Integer.MAX_VALUE),
                        connectedPartCount()
                );
            } else if (previousFormed && !formed) {
                INCore.LOGGER.info(
                        "[ResearchV2] Station disassembled at {} in {}",
                        worldPosition,
                        level.dimension().location()
                );
            }
        }
        return changed;
    }

    public ResearchStationDescriptor describeStation() {
        if (level == null || !formed || stationId.isBlank()) {
            return null;
        }

        List<BlockPos> inventories = java.util.stream.Stream.of(logicHousingPos, researchDrivePos, materialStoragePos, augmenterPos)
                .filter(java.util.Objects::nonNull)
                .toList();
        ResearchStationEndpoints endpoints = new ResearchStationEndpoints(
                powerInputPositions,
                inventories,
                logicHousingPos,
                researchDrivePos,
                materialStoragePos,
                outputPortPositions,
                augmenterPos
        );
        String outputModes = outputPortPositions.isEmpty()
                ? "NONE"
                : outputPortPositions.stream()
                .map(pos -> level.getBlockEntity(pos) instanceof OutputPortBlockEntity outputPort ? outputPort.mode().name() : OutputPortMode.LOGIC.name())
                .collect(java.util.stream.Collectors.joining("+"));

        int mountedDiskTier = 0;
        int mountedDiskSnapshotCount = 0;
        int mountedDiskCorruptedSegmentCount = 0;
        int mountedDiskCorruptedSnapshotCount = 0;
        if (researchDrivePos != null && level.getBlockEntity(researchDrivePos) instanceof ResearchDriveBlockEntity drive) {
            var disk = drive.mountedDisk();
            if (!disk.isEmpty() && StationInventoryRules.isResearchDisk(disk)) {
                mountedDiskTier = switch (ResearchDiskData.readTier(disk)) {
                    case T1 -> 1;
                    case T2 -> 2;
                    case T3 -> 3;
                    case T4 -> 4;
                };
                var snapshots = ResearchDiskData.readSnapshots(disk);
                mountedDiskSnapshotCount = snapshots.size();
                mountedDiskCorruptedSegmentCount = snapshots.stream().mapToInt(snapshot -> snapshot.corruptedSegments().size()).sum();
                mountedDiskCorruptedSnapshotCount = (int) snapshots.stream().filter(snapshot -> !snapshot.corruptedSegments().isEmpty()).count();
            }
        }

        ResearchStationAugmentSummary augmentSummary = ResearchStationServices.computeAugmentSummary(level, this);
        return new ResearchStationDescriptor(
                stationId,
                teamId,
                level.dimension().location().toString(),
                worldPosition.immutable(),
                stationTier(),
                true,
                0,
                0,
                slotCapacity(),
                availableResearchPower(Integer.MAX_VALUE),
                powerFamily,
                powerInputTier,
                outputModes,
                mountedDiskTier,
                mountedDiskSnapshotCount,
                mountedDiskCorruptedSegmentCount,
                mountedDiskCorruptedSnapshotCount,
                augmentSummary.speedMultiplier(),
                augmentSummary.powerMultiplier(),
                augmentSummary.bonusRunChance(),
                augmentSummary.corruptionMultiplier(),
                endpoints,
                connectedParts
        );
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        teamId = tag.getString("teamId");
        formed = tag.getBoolean("formed");
        stationId = tag.getString("stationId");
        powerInputTier = Math.max(0, tag.getInt("powerInputTier"));
        if (tag.contains("powerFamily")) {
            try {
                powerFamily = ResearchPowerFamily.valueOf(tag.getString("powerFamily"));
            } catch (IllegalArgumentException ignored) {
                powerFamily = null;
            }
        } else {
            powerFamily = null;
        }

        long[] packedParts = tag.getLongArray("connectedParts");
        List<BlockPos> loadedParts = new ArrayList<>(packedParts.length);
        for (long packed : packedParts) {
            loadedParts.add(BlockPos.of(packed));
        }
        connectedParts = normalizeConnectedParts(loadedParts);

        long[] packedInputs = tag.getLongArray("powerInputs");
        List<BlockPos> loadedInputs = new ArrayList<>(packedInputs.length);
        for (long packed : packedInputs) {
            loadedInputs.add(BlockPos.of(packed));
        }
        powerInputPositions = normalizeConnectedParts(loadedInputs);
        logicHousingPos = readPos(tag, "logicHousingPos");
        researchDrivePos = readPos(tag, "researchDrivePos");
        materialStoragePos = readPos(tag, "materialStoragePos");
        outputPortPositions = readPositions(tag, "outputPortPositions");
        if (outputPortPositions.isEmpty()) {
            BlockPos legacyOutputPort = readPos(tag, "outputPortPos");
            outputPortPositions = legacyOutputPort == null ? List.of() : List.of(legacyOutputPort);
        }
        augmenterPos = readPos(tag, "augmenterPos");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (!teamId.isBlank()) {
            tag.putString("teamId", teamId);
        }
        tag.putBoolean("formed", formed);
        if (!stationId.isBlank()) {
            tag.putString("stationId", stationId);
        }
        if (powerFamily != null) {
            tag.putString("powerFamily", powerFamily.name());
        }
        tag.putInt("powerInputTier", powerInputTier);

        long[] packedParts = connectedParts.stream().mapToLong(BlockPos::asLong).toArray();
        if (packedParts.length > 0) {
            tag.putLongArray("connectedParts", packedParts);
        }
        long[] packedInputs = powerInputPositions.stream().mapToLong(BlockPos::asLong).toArray();
        if (packedInputs.length > 0) {
            tag.putLongArray("powerInputs", packedInputs);
        }
        writePos(tag, "logicHousingPos", logicHousingPos);
        writePos(tag, "researchDrivePos", researchDrivePos);
        writePos(tag, "materialStoragePos", materialStoragePos);
        writePositions(tag, "outputPortPositions", outputPortPositions);
        writePos(tag, "outputPortPos", outputPortPos());
        writePos(tag, "augmenterPos", augmenterPos);
    }

    private static String buildStationId(ResourceLocation dimensionId, BlockPos controllerPos) {
        return dimensionId + "#" + controllerPos.asLong();
    }

    private static List<BlockPos> normalizeConnectedParts(List<BlockPos> parts) {
        List<BlockPos> normalized = new ArrayList<>();
        if (parts != null) {
            for (BlockPos part : parts) {
                if (part != null) {
                    normalized.add(part.immutable());
                }
            }
        }
        normalized.sort(Comparator.comparingLong(BlockPos::asLong));
        return List.copyOf(normalized);
    }

    private void clearPartBindings() {
        if (level == null) {
            return;
        }
        List<BlockPos> partPositions = new ArrayList<>();
        partPositions.add(logicHousingPos);
        partPositions.add(researchDrivePos);
        partPositions.add(materialStoragePos);
        partPositions.addAll(outputPortPositions);
        partPositions.add(augmenterPos);
        for (BlockPos pos : partPositions.stream().filter(java.util.Objects::nonNull).toList()) {
            if (pos != null && level.getBlockEntity(pos) instanceof AbstractResearchStationPartBlockEntity part) {
                part.clearBinding();
            }
        }
    }

    private void bindPartBindings() {
        if (!formed || level == null) {
            return;
        }
        List<BlockPos> partPositions = new ArrayList<>();
        partPositions.add(logicHousingPos);
        partPositions.add(researchDrivePos);
        partPositions.add(materialStoragePos);
        partPositions.addAll(outputPortPositions);
        partPositions.add(augmenterPos);
        for (BlockPos pos : partPositions.stream().filter(java.util.Objects::nonNull).toList()) {
            if (pos != null && level.getBlockEntity(pos) instanceof AbstractResearchStationPartBlockEntity part) {
                part.bindToController(this);
            }
        }
    }

    private static BlockPos immutablePos(BlockPos pos) {
        return pos == null ? null : pos.immutable();
    }

    private static BlockPos readPos(CompoundTag tag, String key) {
        return tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
    }

    private static List<BlockPos> readPositions(CompoundTag tag, String key) {
        if (!tag.contains(key)) {
            return List.of();
        }
        long[] packed = tag.getLongArray(key);
        List<BlockPos> positions = new ArrayList<>(packed.length);
        for (long value : packed) {
            positions.add(BlockPos.of(value));
        }
        return normalizeConnectedParts(positions);
    }

    private static void writePos(CompoundTag tag, String key, BlockPos pos) {
        if (pos != null) {
            tag.putLong(key, pos.asLong());
        }
    }

    private static void writePositions(CompoundTag tag, String key, List<BlockPos> positions) {
        if (positions != null && !positions.isEmpty()) {
            tag.putLongArray(key, positions.stream().mapToLong(BlockPos::asLong).toArray());
        }
    }
}
