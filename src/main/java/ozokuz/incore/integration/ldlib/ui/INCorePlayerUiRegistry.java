package ozokuz.incore.integration.ldlib.ui;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import ozokuz.incore.integration.ldlib.ui.player.PlayerStatusRouteUiHolder;

public final class INCorePlayerUiRegistry {
    private static final Set<net.minecraft.resources.ResourceLocation> REGISTERED = ConcurrentHashMap.newKeySet();
    private static volatile boolean initialized;

    private INCorePlayerUiRegistry() {
    }

    public static synchronized void registerAll() {
        if (initialized) {
            return;
        }

        register(INCoreUiIds.PLAYER_STATUS, ignored -> new PlayerStatusRouteUiHolder());
        register(INCoreUiIds.PLAYER_LEVEL_REWARDS, ignored -> new PlayerStatusRouteUiHolder());
        register(INCoreUiIds.DUNGEON_DIFFICULTY, ignored -> new PlayerStatusRouteUiHolder());

        initialized = true;
    }

    public static boolean isRegistered(net.minecraft.resources.ResourceLocation routeId) {
        return REGISTERED.contains(routeId);
    }

    private static void register(
            net.minecraft.resources.ResourceLocation routeId,
            java.util.function.Function<net.minecraft.world.entity.player.Player, com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType.PlayerUIHolder> factory
    ) {
        PlayerUIMenuType.register(routeId, factory);
        REGISTERED.add(routeId);
    }
}
