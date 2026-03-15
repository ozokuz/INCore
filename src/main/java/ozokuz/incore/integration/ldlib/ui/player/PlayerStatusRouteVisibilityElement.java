package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

final class PlayerStatusRouteVisibilityElement extends UIElement implements IBindable<String> {
    private final PlayerStatusRouteUiHolder routeHolder;
    private final String routeKey;
    private String currentRouteKey = "";

    PlayerStatusRouteVisibilityElement(PlayerStatusRouteUiHolder routeHolder, ResourceLocation routeId, UIElement view) {
        this.routeHolder = routeHolder;
        this.routeKey = routeId.toString();
        layout(layout -> {
            layout.positionType(dev.vfyjxf.taffy.style.TaffyPosition.ABSOLUTE);
            layout.left(0);
            layout.top(0);
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        addChild(view.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        }));
        setDisplay(false);
        internalSetup();
    }

    @Override
    public String getValue() {
        return currentRouteKey;
    }

    @Override
    public PlayerStatusRouteVisibilityElement setValue(@Nullable String value) {
        currentRouteKey = value == null ? "" : value;
        routeHolder.updateCurrentRoute(currentRouteKey);
        setDisplay(routeKey.equals(currentRouteKey));
        return this;
    }
}
