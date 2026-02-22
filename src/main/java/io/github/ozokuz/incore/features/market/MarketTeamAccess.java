package io.github.ozokuz.incore.features.market;

import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MarketTeamAccess {
    private MarketTeamAccess() {
    }

    public static boolean canAccess(@Nullable UUID ownerId, Player player) {
        if (ownerId == null || player == null) {
            return true;
        }

        UUID playerId = player.getUUID();
        if (ownerId.equals(playerId)) {
            return true;
        }

        try {
            FTBTeamsAPI.API api = FTBTeamsAPI.api();
            if (api == null || !api.isManagerLoaded()) {
                return false;
            }

            var manager = api.getManager();
            var ownerTeam = manager.getTeamForPlayerID(ownerId);
            var playerTeam = manager.getTeamForPlayerID(playerId);
            if (ownerTeam.isEmpty() || playerTeam.isEmpty()) {
                return false;
            }

            return ownerTeam.get().getTeamId().equals(playerTeam.get().getTeamId());
        } catch (Throwable ignored) {
            return false;
        }
    }
}
