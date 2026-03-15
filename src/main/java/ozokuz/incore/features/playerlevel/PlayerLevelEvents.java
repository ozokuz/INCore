package ozokuz.incore.features.playerlevel;

import ozokuz.incore.INCore;
import ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import ozokuz.incore.features.playerlevel.network.PlayerLevelNetworking;
import ozokuz.incore.features.entropy.network.EntropyNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = INCore.MODID)
public class PlayerLevelEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        PlayerLevelManager.initialize(player);
        PlayerLevelManager.grantPendingRewards(player);
        PlayerFeatureUnlockService.reconcile(player);
        PlayerLevelNetworking.syncToPlayer(player);
        EntropyNetworking.syncToPlayer(player);
        BattlePassNetworking.syncToPlayer(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        PlayerLevelManager.copyData(oldPlayer, newPlayer);
        PlayerFeatureUnlockService.reconcile(newPlayer);
        PlayerLevelNetworking.syncToPlayer(newPlayer);
        BattlePassNetworking.syncToPlayer(newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        if (player.serverLevel().getGameTime() % 20L != 0L) {
            return;
        }

        PlayerLevelNetworking.syncToPlayer(player);
        BattlePassNetworking.syncToPlayer(player);
    }
}
