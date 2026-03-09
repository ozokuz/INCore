package io.github.ozokuz.incore.features.roguelike.content;

import com.mojang.serialization.MapCodec;
import io.github.ozokuz.incore.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
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

public class LockedRecoveryStrongboxBlock extends BaseEntityBlock {
    public static final MapCodec<LockedRecoveryStrongboxBlock> CODEC = simpleCodec(LockedRecoveryStrongboxBlock::new);

    public LockedRecoveryStrongboxBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(2.5F)
                .sound(SoundType.WOOD));
    }

    public LockedRecoveryStrongboxBlock(BlockBehaviour.Properties properties) {
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
        return new LockedRecoveryStrongboxBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) {
            return;
        }

        String recoveryId = stack.get(Registration.RECOVERY_STRONGBOX_ID.get());
        if (recoveryId == null || recoveryId.isBlank()) {
            return;
        }

        if (level.getBlockEntity(pos) instanceof LockedRecoveryStrongboxBlockEntity strongbox) {
            strongbox.setRecoveryId(recoveryId);
        }
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull net.minecraft.world.InteractionHand hand,
            @NotNull BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.FAIL;
        }
        if (!stack.is(Registration.RECOVERY_STRONGBOX_KEY_ITEM.get())) {
            return ItemInteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(pos) instanceof LockedRecoveryStrongboxBlockEntity strongbox)) {
            return ItemInteractionResult.FAIL;
        }
        return strongbox.tryUnlock(serverPlayer, stack) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.FAIL;
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof LockedRecoveryStrongboxBlockEntity strongbox && player instanceof ServerPlayer serverPlayer) {
            strongbox.showStatus(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }
}
