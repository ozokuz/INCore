package ozokuz.incore.integration.ldlib.ui;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.holder.ModularUIContainerMenu;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.integration.ldlib.ui.player.PlayerStatusRouteUiHolder;

public final class INCorePlayerUiNavigator {
    private static final Map<UUID, INCoreUiNavigationState> STATES = new ConcurrentHashMap<>();

    private INCorePlayerUiNavigator() {
    }

    public static boolean openRoot(ServerPlayer player, ResourceLocation routeId) {
        return openRoot(player, routeId, INCoreUiRouteContext.Empty.INSTANCE);
    }

    public static boolean openRoot(ServerPlayer player, ResourceLocation routeId, INCoreUiRouteContext context) {
        if (!INCorePlayerUiRegistry.isRegistered(routeId)) {
            return false;
        }
        state(player).openRoot(routeId, context);
        return PlayerUIMenuType.openUI(player, routeId);
    }

    public static boolean pushAndOpen(ServerPlayer player, ResourceLocation routeId) {
        return pushAndOpen(player, routeId, INCoreUiRouteContext.Empty.INSTANCE);
    }

    public static boolean pushAndOpen(ServerPlayer player, ResourceLocation routeId, INCoreUiRouteContext context) {
        if (!INCorePlayerUiRegistry.isRegistered(routeId)) {
            return false;
        }
        state(player).pushAndOpen(routeId, context);
        if (shouldUpdateInPlace(player, routeId)) {
            return true;
        }
        return PlayerUIMenuType.openUI(player, routeId);
    }

    public static boolean goBack(ServerPlayer player) {
        Optional<INCoreUiNavigationState.RouteEntry> previous = state(player).goBack();
        if (previous.isEmpty()) {
            player.closeContainer();
            return false;
        }
        if (shouldUpdateInPlace(player, previous.get().routeId())) {
            return true;
        }
        return PlayerUIMenuType.openUI(player, previous.get().routeId());
    }

    public static Optional<INCoreUiNavigationState.RouteEntry> current(Player player) {
        return state(player).current();
    }

    public static void clear(Player player) {
        state(player).clear();
    }

    private static INCoreUiNavigationState state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new INCoreUiNavigationState());
    }

    private static boolean shouldUpdateInPlace(ServerPlayer player, ResourceLocation routeId) {
        if (!PlayerStatusRouteUiHolder.supportsRoute(routeId)) {
            return false;
        }
        return player.containerMenu instanceof ModularUIContainerMenu menu
                && menu.uiHolder instanceof PlayerStatusRouteUiHolder;
    }
}
