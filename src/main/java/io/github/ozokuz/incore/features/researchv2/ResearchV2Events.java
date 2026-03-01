package io.github.ozokuz.incore.features.researchv2;

import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
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
        if (event.getServer().getTickCount() % 20 != 0) {
            return;
        }

        Set<String> teamIds = new HashSet<>();
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            String teamId = ResearchTeamResolver.resolveTeamId(player);
            if (teamId != null && !teamId.isBlank()) {
                teamIds.add(teamId);
            }
        }

        for (String teamId : teamIds) {
            if (ResearchManager.tickResearch(event.getServer(), teamId)) {
                ResearchV2Networking.syncTeam(event.getServer(), teamId);
            }
        }
    }
}
