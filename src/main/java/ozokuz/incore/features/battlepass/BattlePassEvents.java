package ozokuz.incore.features.battlepass;

import ozokuz.incore.INCore;
import ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = INCore.MODID)
public final class BattlePassEvents {
    private BattlePassEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BattlePassTaskHooks.onPlayerLogin(player);
            BattlePassNetworking.syncToPlayer(player);
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

        String lastLoginDay = oldPlayer.getPersistentData()
                .getCompound(BattlePassProgressManager.KEY_ROOT)
                .getString("last_login_day");

        if (!lastLoginDay.isEmpty()) {
            CompoundTag newRoot = newPlayer.getPersistentData().getCompound(BattlePassProgressManager.KEY_ROOT);
            newRoot.putString("last_login_day", lastLoginDay);
            newPlayer.getPersistentData().put(BattlePassProgressManager.KEY_ROOT, newRoot);
        }
    }
}
