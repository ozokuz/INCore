package ozokuz.incore.features.research;

import ozokuz.incore.INCore;
import ozokuz.incore.features.research.network.ResearchNetworking;
import ozokuz.incore.features.research.state.ResearchNetworkSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = INCore.MODID)
public class ResearchEvents {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchNetworking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Set<String> teamIds = ResearchNetworkSavedData.get(event.getServer()).teamIds();

        for (String teamId : teamIds) {
            if (ResearchManager.tickResearch(event.getServer(), teamId)) {
                ResearchNetworking.syncTeam(event.getServer(), teamId);
            }
        }
    }
}
