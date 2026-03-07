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

public class ResearchControllerBlockEntity extends BlockEntity {
    private static final int REVALIDATE_INTERVAL_TICKS = 20;

    private String teamId = "";
    private int rpBuffer;
    private boolean formed;
    private String stationId = "";
    private List<BlockPos> connectedParts = List.of();
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
            case 1 -> 2_000;
            case 2 -> 8_000;
            case 3 -> 32_000;
            default -> 128_000;
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

        int clampedBuffer = rpBuffer();
        if (clampedBuffer != rpBuffer) {
            rpBuffer = clampedBuffer;
        }

        boolean previousFormed = formed;
        ResearchStationMultiblockValidator.ValidationResult result = ResearchStationMultiblockValidator.validate(level, worldPosition);
        boolean nextFormed = result.formed();
        String nextStationId = nextFormed ? buildStationId(level.dimension().location(), worldPosition) : "";
        List<BlockPos> nextConnectedParts = nextFormed ? normalizeConnectedParts(result.connectedParts()) : List.of();

        boolean changed = formed != nextFormed
                || !stationId.equals(nextStationId)
                || !connectedParts.equals(nextConnectedParts);

        formed = nextFormed;
        stationId = nextStationId;
        connectedParts = nextConnectedParts;

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

        ResearchStationEndpoints endpoints = new ResearchStationEndpoints(worldPosition.immutable(), List.of(), List.of());
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

        long[] packedParts = tag.getLongArray("connectedParts");
        List<BlockPos> loadedParts = new ArrayList<>(packedParts.length);
        for (long packed : packedParts) {
            loadedParts.add(BlockPos.of(packed));
        }
        connectedParts = normalizeConnectedParts(loadedParts);
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

        long[] packedParts = connectedParts.stream().mapToLong(BlockPos::asLong).toArray();
        if (packedParts.length > 0) {
            tag.putLongArray("connectedParts", packedParts);
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
