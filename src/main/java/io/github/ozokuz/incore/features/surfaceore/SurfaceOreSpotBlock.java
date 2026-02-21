package io.github.ozokuz.incore.features.surfaceore;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SurfaceOreSpotBlock extends Block implements EntityBlock {
    private final SurfaceOreType oreType;

    public SurfaceOreSpotBlock(SurfaceOreType oreType) {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .requiresCorrectToolForDrops()
                .strength(1.8F, 6.0F)
                .sound(SoundType.STONE));
        this.oreType = oreType;
    }

    public SurfaceOreType oreType() {
        return oreType;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new SurfaceOreSpotBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (level.isClientSide) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof SurfaceOreSpotBlockEntity be)) {
            return;
        }

        if (be.maxMines() > 0) {
            return;
        }

        be.initializeMines(1);
    }
}
