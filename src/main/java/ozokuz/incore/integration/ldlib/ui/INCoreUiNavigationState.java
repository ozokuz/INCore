package ozokuz.incore.integration.ldlib.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

public final class INCoreUiNavigationState {
    private RouteEntry current;
    private final Deque<RouteEntry> backStack = new ArrayDeque<>();

    public void openRoot(ResourceLocation routeId, INCoreUiRouteContext context) {
        current = new RouteEntry(routeId, context);
        backStack.clear();
    }

    public void pushAndOpen(ResourceLocation routeId, INCoreUiRouteContext context) {
        if (current != null) {
            backStack.push(current);
        }
        current = new RouteEntry(routeId, context);
    }

    public Optional<RouteEntry> goBack() {
        if (backStack.isEmpty()) {
            return Optional.empty();
        }
        current = backStack.pop();
        return Optional.of(current);
    }

    public Optional<RouteEntry> current() {
        return Optional.ofNullable(current);
    }

    public List<RouteEntry> backStackSnapshot() {
        return List.copyOf(backStack);
    }

    public void clear() {
        current = null;
        backStack.clear();
    }

    public record RouteEntry(ResourceLocation routeId, INCoreUiRouteContext context) {
    }
}
