package io.github.ozokuz.incore.features.assembly.content;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoAssemblerT1Block extends HorizontalKineticBlock implements EntityBlock {
    public static final MapCodec<AutoAssemblerT1Block> CODEC = simpleCodec(AutoAssemblerT1Block::new);

    public AutoAssemblerT1Block() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.2F));
    }

    public AutoAssemblerT1Block(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<? extends HorizontalKineticBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new AutoAssemblerT1BlockEntity(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        if (blockEntityType != Registration.AUTO_ASSEMBLER_T1_BE.get()) {
            return null;
        }
        return (lvl, pos, blockState, blockEntity) -> AutoAssemblerT1BlockEntity.tick(lvl, pos, blockState, (AutoAssemblerT1BlockEntity) blockEntity);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(HORIZONTAL_FACING).getAxis();
    }
}
