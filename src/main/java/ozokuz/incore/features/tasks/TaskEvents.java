package ozokuz.incore.features.tasks;

import ozokuz.incore.INCore;
import ozokuz.incore.features.tasks.network.TaskNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = INCore.MODID)
public class TaskEvents {
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TaskService.tick(player);
            TaskNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) {
            return;
        }

        if (!(event.getOriginal() instanceof ServerPlayer oldPlayer)) {
            return;
        }

        TaskService.copyData(oldPlayer, newPlayer);
        TaskService.tick(newPlayer);
        TaskNetworking.syncToPlayer(newPlayer);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.serverLevel().getGameTime() % 20L != 0L) {
            return;
        }

        TaskService.tick(player);
        TaskNetworking.syncToPlayer(player);
    }

    @SubscribeEvent
    public static void onMobDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            TaskService.onMobKill(player, event.getEntity());
            TaskNetworking.syncToPlayer(player);
        }
    }
}
