package io.github.ozokuz.incore.features.researchv2.station;

import io.github.ozokuz.incore.Config;
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
    private int rpBuffer;
    private boolean formed;
    private String stationId = "";
    private List<BlockPos> connectedParts = List.of();
    private List<BlockPos> powerInputPositions = List.of();
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

        if (!formed || level == null || rpBuffer() >= rpCapacity() || powerInputPositions.isEmpty()) {
            return;
        }

        int remaining = rpCapacity() - rpBuffer();
        for (BlockPos inputPos : powerInputPositions) {
            if (remaining <= 0) {
                break;
            }
            BlockEntity blockEntity = level.getBlockEntity(inputPos);
            if (!(blockEntity instanceof IResearchPowerInput input)) {
                continue;
            }
            int pulled = Math.max(0, input.pullResearchPower(this, remaining));
            if (pulled > 0) {
                remaining -= addResearchPower(pulled);
            }
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
        return switch (Math.max(1, stationTier())) {
            case 1 -> Config.CONTROLLER_BUFFER_T1.get();
            case 2 -> Config.CONTROLLER_BUFFER_T2.get();
            case 3 -> Config.CONTROLLER_BUFFER_T3.get();
            default -> Config.CONTROLLER_BUFFER_T4.get();
        };
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
        return Math.max(0, Math.min(rpCapacity(), rpBuffer));
    }

    public int addResearchPower(int amount) {
        int requested = Math.max(0, amount);
        if (requested <= 0) {
            return 0;
        }

        int before = rpBuffer();
        int after = Math.min(rpCapacity(), before + requested);
        rpBuffer = after;
        if (after != before) {
            setChanged();
        }
        return after - before;
    }

    public int consumeResearchPower(int amount) {
        int requested = Math.max(0, amount);
        int available = rpBuffer();
        if (requested <= 0 || available <= 0) {
            return 0;
        }

        int consumed = Math.min(requested, available);
        rpBuffer = available - consumed;
        if (consumed > 0) {
            setChanged();
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

    public int connectedPartCount() {
        return connectedParts.size();
    }

    public boolean revalidateStructure() {
        if (level == null || level.isClientSide) {
            return false;
        }
        ResearchMultiblockStationRegistry.register(this);

        int clampedBuffer = rpBuffer();
        if (clampedBuffer != rpBuffer) {
            rpBuffer = clampedBuffer;
        }

        boolean previousFormed = formed;
        ResearchStationTopology result = ResearchStationMultiblockValidator.validate(level, worldPosition);
        boolean nextFormed = result.formed();
        String nextStationId = nextFormed ? buildStationId(level.dimension().location(), worldPosition) : "";
        List<BlockPos> nextConnectedParts = nextFormed ? normalizeConnectedParts(result.connectedParts()) : List.of();
        List<BlockPos> nextPowerInputPositions = nextFormed ? normalizeConnectedParts(result.inputPositions()) : List.of();
        ResearchPowerFamily nextPowerFamily = nextFormed ? result.powerFamily() : null;
        int nextPowerInputTier = nextFormed ? Math.max(0, result.powerInputTier()) : 0;

        boolean changed = formed != nextFormed
                || !stationId.equals(nextStationId)
                || !connectedParts.equals(nextConnectedParts)
                || !powerInputPositions.equals(nextPowerInputPositions)
                || powerFamily != nextPowerFamily
                || powerInputTier != nextPowerInputTier;

        formed = nextFormed;
        stationId = nextStationId;
        connectedParts = nextConnectedParts;
        powerInputPositions = nextPowerInputPositions;
        powerFamily = nextPowerFamily;
        powerInputTier = nextPowerInputTier;

        if (changed) {
            setChanged();

            if (!previousFormed && formed) {
                INCore.LOGGER.info(
                        "[ResearchV2] Station formed id={} tier={} buffer={}/{} parts={}",
                        stationId,
                        stationTier(),
                        rpBuffer(),
                        rpCapacity(),
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

        ResearchStationEndpoints endpoints = new ResearchStationEndpoints(powerInputPositions, List.of());
        return new ResearchStationDescriptor(
                stationId,
                teamId,
                level.dimension().location().toString(),
                worldPosition.immutable(),
                stationTier(),
                true,
                rpBuffer(),
                rpCapacity(),
                slotCapacity(),
                powerFamily,
                powerInputTier,
                endpoints,
                connectedParts
        );
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        teamId = tag.getString("teamId");
        rpBuffer = Math.max(0, tag.getInt("rpBuffer"));
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
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);

        if (!teamId.isBlank()) {
            tag.putString("teamId", teamId);
        }
        tag.putInt("rpBuffer", rpBuffer());
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
}
