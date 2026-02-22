package io.github.ozokuz.incore.features.market.content;

import com.mojang.serialization.MapCodec;
import dev.ithundxr.createnumismatics.content.bank.IDCardItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import io.github.ozokuz.incore.Registration;

import java.util.UUID;

public class ShipmentTerminalBlock extends BaseEntityBlock {
    public static final MapCodec<ShipmentTerminalBlock> CODEC = simpleCodec(ShipmentTerminalBlock::new);

    public ShipmentTerminalBlock() {
        this(Properties.of().mapColor(MapColor.METAL).strength(3.2F).sound(SoundType.METAL));
    }

    public ShipmentTerminalBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ShipmentTerminalBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType) {
        return createTickerHelper(blockEntityType, Registration.SHIPMENT_TERMINAL_BE.get(), ShipmentTerminalBlockEntity::tick);
    }

    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ShipmentTerminalBlockEntity terminal) {
            terminal.setOwner(player.getUUID());
        }
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull net.minecraft.world.phys.BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.CONSUME;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof ShipmentTerminalBlockEntity terminal) {
            if (!terminal.canAccess(player)) {
                player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
                return InteractionResult.FAIL;
            }
            serverPlayer.openMenu(terminal, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(
            @NotNull ItemStack stack,
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Player player,
            @NotNull InteractionHand hand,
            @NotNull net.minecraft.world.phys.BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof ShipmentTerminalBlockEntity terminal)) {
            return ItemInteractionResult.CONSUME;
        }

        if (player.isShiftKeyDown() && IDCardItem.isBound(stack) && terminal.canManageTrust(player.getUUID())) {
            UUID trusted = IDCardItem.get(stack);
            if (trusted != null) {
                boolean added = terminal.toggleTrusted(trusted);
                player.sendSystemMessage(Component.translatable(
                        added ? "incore.market.trust.added" : "incore.market.trust.removed",
                        trusted.toString()
                ));
            }
            return ItemInteractionResult.SUCCESS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            if (!terminal.canAccess(player)) {
                player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
                return ItemInteractionResult.FAIL;
            }
            serverPlayer.openMenu(terminal, pos);
        }
        return ItemInteractionResult.CONSUME;
    }
}
