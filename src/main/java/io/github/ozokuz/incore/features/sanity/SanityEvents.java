package io.github.ozokuz.incore.features.sanity;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = INCore.MODID)
public class SanityEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        SanityManager.getCurrentSanity(player);
        SanityNetworking.syncToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        SanityManager.copyData(oldPlayer, newPlayer);
        SanityNetworking.syncToPlayer(newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.serverLevel().getGameTime() % 20 != 0) {
            return;
        }

        SanityNetworking.syncToPlayer(player);
    }
}
