package io.github.ozokuz.incore.features.arena;

import dev.shadowsoffire.gateways.event.GateEvent;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateBlockEntity;
import io.github.ozokuz.incore.features.arena.content.ArenaRewardCrateData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = INCore.MODID)
public class ArenaEvents {
    private static final String ARENA_NO_DROP_TAG = "incore:arena_no_default_drops";

    @SubscribeEvent
    public static void onGatewayCompleted(GateEvent.Completed event) {
        ArenaService.onGatewayCompleted(event.getEntity());
    }

    @SubscribeEvent
    public static void onGatewayFailed(GateEvent.Failed event) {
        ArenaService.onGatewayFailed(event.getEntity());
    }

    @SubscribeEvent
    public static void onWaveEntitySpawned(GateEvent.WaveEntitySpawned event) {
        if (!ArenaService.isArenaGateway(event.getEntity().getUUID())) {
            return;
        }

        event.getWaveEntity().getPersistentData().putBoolean(ARENA_NO_DROP_TAG, true);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity().getPersistentData().getBoolean(ARENA_NO_DROP_TAG)) {
            event.getDrops().clear();
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (event.getLevel().getBlockState(event.getPos()).getBlock() != Registration.ARENA_REWARD_CRATE_BLOCK.get()) {
            return;
        }

        if (!player.isShiftKeyDown()) {
            return;
        }

        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            player.sendSystemMessage(Component.translatable("incore.arena.crate.empty_hand_required"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof ArenaRewardCrateBlockEntity crateBlockEntity)) {
            player.sendSystemMessage(Component.translatable("incore.arena.crate.invalid"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        ArenaRewardCrateData.CrateContents contents = crateBlockEntity.getContents();
        if (contents == null) {
            player.sendSystemMessage(Component.translatable("incore.arena.crate.invalid"));
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
            return;
        }

        boolean opened = ArenaRewardCrateData.tryOpen(player, contents);
        if (opened) {
            event.getLevel().removeBlock(event.getPos(), false);
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
            return;
        }

        event.setCancellationResult(InteractionResult.FAIL);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaService.onPlayerDeath(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaService.onPlayerRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaService.onPlayerLogin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ArenaService.onPlayerLogout(player);
        }
    }
}
