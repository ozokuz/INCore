package ozokuz.incore.integration.ldlib.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;

final class INCoreUiRouteAccess {
    private INCoreUiRouteAccess() {
    }

    static boolean canOpen(ServerPlayer player, ResourceLocation routeId) {
        ResourceLocation requiredFeature = requiredFeature(routeId);
        if (requiredFeature == null) {
            return true;
        }
        if (PlayerFeatureUnlockService.hasUnlocked(player, requiredFeature)) {
            return true;
        }
        player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(requiredFeature));
        return false;
    }

    static Component unavailableMessage(ResourceLocation routeId) {
        return Component.literal("UI route is not available yet: " + routeId);
    }

    private static ResourceLocation requiredFeature(ResourceLocation routeId) {
        return switch (routeId.toString()) {
            case "incore:ui/shop" -> PlayerFeatureUnlockIds.SHOP_SCREEN;
            case "incore:ui/gacha", "incore:ui/gacha_info", "incore:ui/gacha_guaranteed_selection" -> PlayerFeatureUnlockIds.GACHA_BASIC;
            case "incore:ui/battle_pass" -> PlayerFeatureUnlockIds.BATTLEPASS_SCREEN;
            case "incore:ui/task_overview" -> PlayerFeatureUnlockIds.TASKS_SCREEN;
            case "incore:ui/combat_catalog" -> PlayerFeatureUnlockIds.ARENA_TIER_1;
            default -> null;
        };
    }
}
