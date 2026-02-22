package io.github.ozokuz.incore.features.surfaceore;

import io.github.ozokuz.incore.INCore;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = INCore.MODID)
public final class SurfaceOreMiningEvents {
    private static final long DESTROY_CONFIRM_WINDOW_TICKS = 80L;
    private static final int DESTROY_CONFIRM_WINDOW_SECONDS = 4;
    private static final Map<DestroyAttemptKey, Long> DESTROY_CONFIRMATIONS = new HashMap<>();

    private SurfaceOreMiningEvents() {
    }

    @SubscribeEvent
    public static void onBreakBlock(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }

        Level level = player.level();
        if (level.isClientSide) {
            return;
        }

        Block eventBlock = event.getState().getBlock();
        SurfaceOreSpotBlock oreSpotBlock = eventBlock instanceof SurfaceOreSpotBlock spot ? spot : null;
        SurfaceStoneSpotBlock stoneSpotBlock = eventBlock instanceof SurfaceStoneSpotBlock spot ? spot : null;
        if (oreSpotBlock == null && stoneSpotBlock == null) {
            return;
        }

        cleanupExpiredConfirmations(level.getGameTime());

        if (player.isShiftKeyDown()) {
            if (player.isCreative()) {
                clearDestroyConfirmation(player.getUUID(), level.dimension().location(), event.getPos().asLong());
                return;
            }

            if (!hasValidDestroyConfirmation(player, event.getPos().asLong())) {
                setDestroyConfirmation(player, event.getPos().asLong(), level.getGameTime());
                event.setCanceled(true);
                player.displayClientMessage(
                        Component.translatable("incore.surface_ore.destroy_confirm", DESTROY_CONFIRM_WINDOW_SECONDS),
                        false
                );
                return;
            }

            clearDestroyConfirmation(player.getUUID(), level.dimension().location(), event.getPos().asLong());
            return;
        }

        clearDestroyConfirmation(player.getUUID(), level.dimension().location(), event.getPos().asLong());

        if (stoneSpotBlock != null) {
            event.setCanceled(true);
            if (level instanceof ServerLevel serverLevel) {
                for (ItemStack drop : stoneSpotBlock.stoneType().miningDrops(serverLevel, event.getPos(), player, player.getMainHandItem())) {
                    Containers.dropItemStack(
                            level,
                            event.getPos().getX() + 0.5D,
                            event.getPos().getY() + 0.5D,
                            event.getPos().getZ() + 0.5D,
                            drop
                    );
                }
            }
            player.displayClientMessage(
                    Component.translatable("incore.surface_stone.mined_infinite", event.getState().getBlock().getName()),
                    true
            );
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof SurfaceOreSpotBlockEntity spotBE)) {
            return;
        }

        if (spotBE.maxMines() <= 0 || spotBE.remainingMines() <= 0) {
            return;
        }

        if (spotBE.remainingMines() == 1) {
            player.displayClientMessage(
                    Component.translatable("incore.surface_ore.mined", event.getState().getBlock().getName(), 0, spotBE.maxMines()),
                    true
            );
            player.displayClientMessage(
                    Component.translatable("incore.surface_ore.depleted", event.getState().getBlock().getName()),
                    false
            );
            return;
        }

        SurfaceOreSpotBlockEntity.MiningResult result = spotBE.consumeMine();
        if (!result.success()) {
            return;
        }

        event.setCanceled(true);
        Containers.dropItemStack(
                level,
                event.getPos().getX() + 0.5D,
                event.getPos().getY() + 0.5D,
                event.getPos().getZ() + 0.5D,
                oreSpotBlock.oreType().oreDropStack()
        );
        player.displayClientMessage(
                Component.translatable("incore.surface_ore.mined", event.getState().getBlock().getName(), result.remainingMines(), result.maxMines()),
                true
        );
    }

    private static boolean hasValidDestroyConfirmation(ServerPlayer player, long pos) {
        ResourceLocation dimensionId = player.level().dimension().location();
        DestroyAttemptKey key = new DestroyAttemptKey(player.getUUID(), dimensionId, pos);
        Long confirmedAt = DESTROY_CONFIRMATIONS.get(key);
        if (confirmedAt == null) {
            return false;
        }
        if (player.level().getGameTime() - confirmedAt > DESTROY_CONFIRM_WINDOW_TICKS) {
            DESTROY_CONFIRMATIONS.remove(key);
            return false;
        }
        return true;
    }

    private static void setDestroyConfirmation(ServerPlayer player, long pos, long gameTime) {
        ResourceLocation dimensionId = player.level().dimension().location();
        DESTROY_CONFIRMATIONS.put(new DestroyAttemptKey(player.getUUID(), dimensionId, pos), gameTime);
    }

    private static void clearDestroyConfirmation(UUID playerId, ResourceLocation dimensionId, long pos) {
        DESTROY_CONFIRMATIONS.remove(new DestroyAttemptKey(playerId, dimensionId, pos));
    }

    private static void cleanupExpiredConfirmations(long currentGameTime) {
        DESTROY_CONFIRMATIONS.entrySet().removeIf(entry -> currentGameTime - entry.getValue() > DESTROY_CONFIRM_WINDOW_TICKS);
    }

    private record DestroyAttemptKey(UUID playerId, ResourceLocation dimensionId, long pos) {
    }
}
