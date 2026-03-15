package ozokuz.incore.features.market.content;

import com.mojang.serialization.MapCodec;
import ozokuz.incore.features.market.MarketService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MarketTerminalMeBlock extends BaseEntityBlock {
    public static final MapCodec<MarketTerminalMeBlock> CODEC = simpleCodec(MarketTerminalMeBlock::new);

    public MarketTerminalMeBlock() {
        this(Properties.of().mapColor(MapColor.METAL).strength(3.8F).sound(SoundType.METAL));
    }

    public MarketTerminalMeBlock(Properties properties) {
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
        return new MarketTerminalMeBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide || !(placer instanceof Player player)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MarketTerminalMeBlockEntity terminal) {
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
        if (blockEntity instanceof MarketTerminalMeBlockEntity terminal) {
            if (!terminal.canTrade(player)) {
                player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
                return InteractionResult.FAIL;
            }
            if (player.isShiftKeyDown()) {
                serverPlayer.openMenu(terminal, pos);
                return InteractionResult.CONSUME;
            }
            MarketService.openTerminalScreen(serverPlayer, terminal);
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

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return ItemInteractionResult.CONSUME;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof MarketTerminalMeBlockEntity terminal)) {
            return ItemInteractionResult.CONSUME;
        }

        if (!terminal.canTrade(player)) {
            player.sendSystemMessage(Component.translatable("incore.market.not_allowed"));
            return ItemInteractionResult.FAIL;
        }

        if (player.isShiftKeyDown()) {
            serverPlayer.openMenu(terminal, pos);
            return ItemInteractionResult.SUCCESS;
        }

        MarketService.openTerminalScreen(serverPlayer, terminal);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MarketTerminalMeBlockEntity terminal) {
                Containers.dropItemStack(
                        level,
                        pos.getX() + 0.5D,
                        pos.getY() + 0.5D,
                        pos.getZ() + 0.5D,
                        terminal.cardStack().copy()
                );
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
