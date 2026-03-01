package io.github.ozokuz.incore.features.researchv2;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Set;

@EventBusSubscriber(modid = INCore.MODID)
public class ResearchV2Events {
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ResearchV2Networking.syncToPlayer(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Set<String> teamIds = ResearchNetworkSavedData.get(event.getServer()).teamIds();

        for (String teamId : teamIds) {
            if (ResearchManager.tickResearch(event.getServer(), teamId)) {
                ResearchV2Networking.syncTeam(event.getServer(), teamId);
            }
        }
    }
}
