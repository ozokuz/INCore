package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.researchv2.station.network.StationNetworkService;
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

public class ResearchOrchestratorControllerBlockEntity extends BlockEntity {
    private static final int REVALIDATE_INTERVAL_TICKS = 20;

    private String teamId = "";
    private boolean formed;
    private String orchestratorId = "";
    private List<BlockPos> connectedParts = List.of();
    private List<BlockPos> powerInputPositions = List.of();
    private List<BlockPos> linkingPortPositions = List.of();
    private BlockPos wirelessLinkPos;
    private BlockPos orchestrationDrivePos;
    private BlockPos augmenterPos;
    private ResearchPowerFamily powerFamily;
    private int powerInputTier;
    private int tickCounter;

    public ResearchOrchestratorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.RESEARCH_ORCHESTRATOR_CONTROLLER_BE.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            ResearchOrchestratorRegistry.register(this);
            revalidateStructure();
            StationNetworkService.onTopologyChanged(level);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            ResearchOrchestratorRegistry.unregister(this);
            StationNetworkService.onTopologyChanged(level);
        }
        super.setRemoved();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ResearchOrchestratorControllerBlockEntity orchestrator) {
        if (level.isClientSide) {
            return;
        }
        orchestrator.serverTick();
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
            ResearchOrchestratorRegistry.register(this);
            StationNetworkService.onTopologyChanged(level);
        }
        setChanged();
    }

    public boolean isFormed() {
        return formed;
    }

    public String orchestratorId() {
        return orchestratorId;
    }

    public List<BlockPos> connectedParts() {
        return connectedParts;
    }

    public List<BlockPos> powerInputPositions() {
        return powerInputPositions;
    }

    public List<BlockPos> linkingPortPositions() {
        return linkingPortPositions;
    }

    public BlockPos wirelessLinkPos() {
        return wirelessLinkPos;
    }

    public BlockPos orchestrationDrivePos() {
        return orchestrationDrivePos;
    }

    public BlockPos augmenterPos() {
        return augmenterPos;
    }

    public ResearchPowerFamily powerFamily() {
        return powerFamily;
    }

    public int powerInputTier() {
        return powerInputTier;
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

            int fromInput = Math.max(0, input.availableResearchPower(null, remaining));
            if (fromInput > 0) {
                available += fromInput;
                remaining -= fromInput;
            }
        }
        return available;
    }

    public boolean hasMountedOrchestrationDisk() {
        if (!formed || level == null || orchestrationDrivePos == null) {
            return false;
        }
        if (!(level.getBlockEntity(orchestrationDrivePos) instanceof OrchestrationDriveBlockEntity drive)) {
            return false;
        }
        return !drive.mountedDisk().isEmpty() && StationInventoryRules.isOrchestrationDisk(drive.mountedDisk());
    }

    public boolean revalidateStructure() {
        if (level == null || level.isClientSide) {
            return false;
        }
        ResearchOrchestratorRegistry.register(this);
        boolean previousFormed = formed;
        ResearchOrchestratorTopology result = ResearchOrchestratorMultiblockValidator.validate(level, worldPosition);
        boolean nextFormed = result.formed();
        String nextId = nextFormed ? buildOrchestratorId(level.dimension().location(), worldPosition) : "";
        List<BlockPos> nextConnectedParts = nextFormed ? normalizePositions(result.connectedParts()) : List.of();
        List<BlockPos> nextPowerInputs = nextFormed ? normalizePositions(result.powerInputPositions()) : List.of();
        List<BlockPos> nextLinkingPorts = nextFormed ? normalizePositions(result.linkingPortPositions()) : List.of();
        BlockPos nextWirelessLinkPos = nextFormed ? immutablePos(result.wirelessLinkPos()) : null;
        BlockPos nextDrivePos = nextFormed ? immutablePos(result.orchestrationDrivePos()) : null;
        BlockPos nextAugmenterPos = nextFormed ? immutablePos(result.augmenterPos()) : null;
        ResearchPowerFamily nextPowerFamily = nextFormed ? result.powerFamily() : null;
        int nextPowerTier = nextFormed ? Math.max(0, result.powerInputTier()) : 0;

        boolean changed = formed != nextFormed
                || !orchestratorId.equals(nextId)
                || !connectedParts.equals(nextConnectedParts)
                || !powerInputPositions.equals(nextPowerInputs)
                || !linkingPortPositions.equals(nextLinkingPorts)
                || !java.util.Objects.equals(wirelessLinkPos, nextWirelessLinkPos)
                || !java.util.Objects.equals(orchestrationDrivePos, nextDrivePos)
                || !java.util.Objects.equals(augmenterPos, nextAugmenterPos)
                || powerFamily != nextPowerFamily
                || powerInputTier != nextPowerTier;

        if (changed) {
            clearPartBindings();
            formed = nextFormed;
            orchestratorId = nextId;
            connectedParts = nextConnectedParts;
            powerInputPositions = nextPowerInputs;
            linkingPortPositions = nextLinkingPorts;
            wirelessLinkPos = nextWirelessLinkPos;
            orchestrationDrivePos = nextDrivePos;
            augmenterPos = nextAugmenterPos;
            powerFamily = nextPowerFamily;
            powerInputTier = nextPowerTier;
            bindPartBindings();
        }

        if (changed) {
            setChanged();
            StationNetworkService.onTopologyChanged(level);
        }
        return changed;
    }

    public ResearchOrchestratorDescriptor describeOrchestrator() {
        if (level == null || !formed || orchestratorId.isBlank()) {
            return null;
        }
        return new ResearchOrchestratorDescriptor(
                orchestratorId,
                teamId,
                level.dimension().location().toString(),
                worldPosition.immutable(),
                true,
                powerFamily,
                powerInputTier,
                powerInputPositions,
                linkingPortPositions,
                wirelessLinkPos,
                orchestrationDrivePos,
                augmenterPos
        );
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        teamId = tag.getString("teamId");
        formed = tag.getBoolean("formed");
        orchestratorId = tag.getString("orchestratorId");
        if (tag.contains("powerFamily")) {
            try {
                powerFamily = ResearchPowerFamily.valueOf(tag.getString("powerFamily"));
            } catch (IllegalArgumentException ignored) {
                powerFamily = null;
            }
        } else {
            powerFamily = null;
        }
        powerInputTier = Math.max(0, tag.getInt("powerInputTier"));
        connectedParts = readPositions(tag, "connectedParts");
        powerInputPositions = readPositions(tag, "powerInputs");
        linkingPortPositions = readPositions(tag, "linkingPorts");
        wirelessLinkPos = readPos(tag, "wirelessLinkPos");
        orchestrationDrivePos = readPos(tag, "orchestrationDrivePos");
        augmenterPos = readPos(tag, "augmenterPos");
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (!teamId.isBlank()) {
            tag.putString("teamId", teamId);
        }
        tag.putBoolean("formed", formed);
        if (!orchestratorId.isBlank()) {
            tag.putString("orchestratorId", orchestratorId);
        }
        if (powerFamily != null) {
            tag.putString("powerFamily", powerFamily.name());
        }
        tag.putInt("powerInputTier", powerInputTier);
        writePositions(tag, "connectedParts", connectedParts);
        writePositions(tag, "powerInputs", powerInputPositions);
        writePositions(tag, "linkingPorts", linkingPortPositions);
        writePos(tag, "wirelessLinkPos", wirelessLinkPos);
        writePos(tag, "orchestrationDrivePos", orchestrationDrivePos);
        writePos(tag, "augmenterPos", augmenterPos);
    }

    private void clearPartBindings() {
        if (level == null) {
            return;
        }
        for (BlockPos pos : linkingPortPositions) {
            if (level.getBlockEntity(pos) instanceof LinkingPortBlockEntity port) {
                port.clearAttachment();
            }
        }
        if (wirelessLinkPos != null && level.getBlockEntity(wirelessLinkPos) instanceof WirelessLinkBlockEntity wireless) {
            wireless.clearBinding();
        }
        if (orchestrationDrivePos != null && level.getBlockEntity(orchestrationDrivePos) instanceof AbstractResearchStationPartBlockEntity part) {
            part.clearBinding();
        }
        if (augmenterPos != null && level.getBlockEntity(augmenterPos) instanceof AbstractResearchStationPartBlockEntity part) {
            part.clearBinding();
        }
    }

    private void bindPartBindings() {
        if (!formed || level == null) {
            return;
        }
        for (BlockPos pos : linkingPortPositions) {
            if (level.getBlockEntity(pos) instanceof LinkingPortBlockEntity port) {
                port.setAttachment(LinkOwnerKind.ORCHESTRATOR, orchestratorId, teamId);
            }
        }
        if (wirelessLinkPos != null && level.getBlockEntity(wirelessLinkPos) instanceof WirelessLinkBlockEntity wireless) {
            wireless.bindToOrchestrator(this);
        }
        if (orchestrationDrivePos != null && level.getBlockEntity(orchestrationDrivePos) instanceof AbstractResearchStationPartBlockEntity part) {
            part.bindToOrchestrator(this);
        }
        if (augmenterPos != null && level.getBlockEntity(augmenterPos) instanceof AbstractResearchStationPartBlockEntity part) {
            part.bindToOrchestrator(this);
        }
    }

    private static String buildOrchestratorId(ResourceLocation dimensionId, BlockPos pos) {
        return dimensionId + "#research_orchestrator#" + pos.asLong();
    }

    private static BlockPos immutablePos(BlockPos pos) {
        return pos == null ? null : pos.immutable();
    }

    private static List<BlockPos> normalizePositions(List<BlockPos> positions) {
        List<BlockPos> normalized = new ArrayList<>();
        if (positions != null) {
            for (BlockPos pos : positions) {
                if (pos != null) {
                    normalized.add(pos.immutable());
                }
            }
        }
        normalized.sort(Comparator.comparingLong(BlockPos::asLong));
        return List.copyOf(normalized);
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
        return normalizePositions(positions);
    }

    private static BlockPos readPos(CompoundTag tag, String key) {
        return tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
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
