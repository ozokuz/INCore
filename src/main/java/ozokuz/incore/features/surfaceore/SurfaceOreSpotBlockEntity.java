package ozokuz.incore.features.surfaceore;

import ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SurfaceOreSpotBlockEntity extends BlockEntity {
    private static final String KEY_MAX_MINES = "max_mines";
    private static final String KEY_REMAINING_MINES = "remaining_mines";

    private int maxMines;
    private int remainingMines;

    public SurfaceOreSpotBlockEntity(BlockPos pos, BlockState blockState) {
        super(Registration.SURFACE_ORE_SPOT_BE.get(), pos, blockState);
    }

    public int maxMines() {
        return maxMines;
    }

    public int remainingMines() {
        return remainingMines;
    }

    public void initializeMines(int mines) {
        int normalized = Math.max(1, mines);
        maxMines = normalized;
        remainingMines = normalized;
        setChanged();
        syncToClient();
    }

    public MiningResult consumeMine() {
        if (remainingMines <= 0 || maxMines <= 0) {
            return new MiningResult(false, Math.max(0, remainingMines), Math.max(0, maxMines), true);
        }

        remainingMines = Math.max(0, remainingMines - 1);
        setChanged();
        syncToClient();
        return new MiningResult(true, remainingMines, maxMines, remainingMines <= 0);
    }

    private void syncToClient() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockState state = getBlockState();
        serverLevel.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.loadAdditional(tag, registries);
        maxMines = Math.max(0, tag.getInt(KEY_MAX_MINES));
        remainingMines = Math.max(0, tag.getInt(KEY_REMAINING_MINES));
        if (maxMines > 0 && remainingMines > maxMines) {
            remainingMines = maxMines;
        }
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
        super.saveAdditional(tag, registries);
        if (maxMines > 0) {
            tag.putInt(KEY_MAX_MINES, maxMines);
            tag.putInt(KEY_REMAINING_MINES, remainingMines);
        }
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(HolderLookup.@NotNull Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public record MiningResult(boolean success, int remainingMines, int maxMines, boolean depleted) {
    }
}
