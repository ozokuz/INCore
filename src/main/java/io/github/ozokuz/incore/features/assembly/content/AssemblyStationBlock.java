package io.github.ozokuz.incore.features.assembly.content;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssemblyStationBlock extends BaseEntityBlock {
    public static final MapCodec<AssemblyStationBlock> CODEC = simpleCodec(AssemblyStationBlock::new);

    public AssemblyStationBlock() {
        this(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0F).sound(SoundType.METAL));
    }

    public AssemblyStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new AssemblyStationBlockEntity(pos, state);
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof AssemblyStationBlockEntity station && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(station, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull net.minecraft.world.InteractionHand hand, @NotNull BlockHitResult hitResult) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof AssemblyStationBlockEntity station && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(station, pos);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof AssemblyStationBlockEntity station) {
            station.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
