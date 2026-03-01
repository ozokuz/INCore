package io.github.ozokuz.incore.features.researchv2.team;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class ResearchTeamResolver {
    private ResearchTeamResolver() {
    }

    public static @Nullable String resolveTeamId(ServerPlayer player) {
        try {
            FTBTeamsAPI.API api = FTBTeamsAPI.api();
            if (api == null || !api.isManagerLoaded()) {
                return personalTeamId(player);
            }

            var optionalTeam = api.getManager().getTeamForPlayerID(player.getUUID());
            if (optionalTeam.isEmpty()) {
                return personalTeamId(player);
            }

            UUID teamId = optionalTeam.get().getTeamId();
            return teamId == null ? personalTeamId(player) : teamId.toString();
        } catch (Throwable ignored) {
            return personalTeamId(player);
        }
    }

    private static String personalTeamId(ServerPlayer player) {
        return "player:" + player.getUUID();
    }
}
