package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType;
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.INCorePlayerUiNavigator;
import ozokuz.incore.integration.ldlib.ui.INCoreUiIds;

public final class PlayerStatusRouteUiHolder implements PlayerUIMenuType.PlayerUIHolder {
    private static final Set<ResourceLocation> ROUTES = Set.of(
            INCoreUiIds.PLAYER_STATUS,
            INCoreUiIds.PLAYER_LEVEL_REWARDS,
            INCoreUiIds.DUNGEON_DIFFICULTY,
            INCoreUiIds.TASK_OVERVIEW,
            INCoreUiIds.PARTY_MANAGEMENT,
            INCoreUiIds.COMBAT_CATALOG
    );
    private volatile ResourceLocation currentRouteId = INCoreUiIds.PLAYER_STATUS;

    @Override
    public ModularUI createUI(Player player) {
        UIElement root = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        root.addChildren(
                routeView(player, INCoreUiIds.PLAYER_STATUS, PlayerStatusUiHolder.createView(player)),
                routeView(player, INCoreUiIds.PLAYER_LEVEL_REWARDS, PlayerLevelRewardsUiHolder.createView(player)),
                routeView(player, INCoreUiIds.DUNGEON_DIFFICULTY, DungeonDifficultyUiHolder.createView(player)),
                routeView(player, INCoreUiIds.TASK_OVERVIEW, TaskOverviewUiHolder.createView(player)),
                routeView(player, INCoreUiIds.PARTY_MANAGEMENT, PartyManagementUiHolder.createView(player)),
                routeView(player, INCoreUiIds.COMBAT_CATALOG, CombatCatalogUiHolder.createView(player))
        );
        return INCoreLdLibUiScaffold.build(player, root);
    }

    public static boolean supportsRoute(ResourceLocation routeId) {
        return ROUTES.contains(routeId);
    }

    public boolean canGoBackOnEscape() {
        return !INCoreUiIds.PLAYER_STATUS.equals(currentRouteId);
    }

    void updateCurrentRoute(String routeKey) {
        ResourceLocation routeId = ResourceLocation.tryParse(routeKey);
        currentRouteId = routeId == null ? INCoreUiIds.PLAYER_STATUS : routeId;
    }

    private UIElement routeView(Player player, ResourceLocation routeId, UIElement view) {
        PlayerStatusRouteVisibilityElement wrapper = new PlayerStatusRouteVisibilityElement(this, routeId, view);
        wrapper.bind(DataBindingBuilder.stringS2C(() -> currentRouteKey(player)).build());
        return wrapper;
    }

    private static String currentRouteKey(Player player) {
        return INCorePlayerUiNavigator.current(player)
                .map(entry -> entry.routeId().toString())
                .orElse(INCoreUiIds.PLAYER_STATUS.toString());
    }
}
