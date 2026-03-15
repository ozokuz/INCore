package ozokuz.incore.features.entropy;

import ozokuz.incore.INCore;
import ozokuz.incore.features.entropy.network.EntropyNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = INCore.MODID)
public class EntropyEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        EntropyManager.getCurrentEntropy(player);
        EntropyNetworking.syncToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        EntropyManager.copyData(oldPlayer, newPlayer);
        EntropyNetworking.syncToPlayer(newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.serverLevel().getGameTime() % 20 != 0) {
            return;
        }

        EntropyNetworking.syncToPlayer(player);
    }
}
