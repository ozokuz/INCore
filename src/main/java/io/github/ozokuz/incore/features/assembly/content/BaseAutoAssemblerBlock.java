package io.github.ozokuz.incore.features.assembly.content;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public abstract class BaseAutoAssemblerBlock extends BaseEntityBlock {
    protected static Properties defaultProperties() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.2F).sound(SoundType.METAL);
    }

    protected BaseAutoAssemblerBlock() {
        this(defaultProperties());
    }

    protected BaseAutoAssemblerBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull net.minecraft.world.InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, pos);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AutoAssemblerSupport support) {
            support.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public interface AutoAssemblerSupport {
        void dropContents();
    }
}
