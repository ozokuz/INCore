package ozokuz.incore.integration.ldlib.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class INCoreUiNavigationStateTest {
    @Test
    void openRootClearsExistingBackStack() {
        INCoreUiNavigationState state = new INCoreUiNavigationState();

        state.openRoot(INCoreUiIds.PLAYER_STATUS, INCoreUiRouteContext.Empty.INSTANCE);
        state.pushAndOpen(INCoreUiIds.PLAYER_LEVEL_REWARDS, INCoreUiRouteContext.Empty.INSTANCE);
        state.openRoot(INCoreUiIds.DUNGEON_DIFFICULTY, INCoreUiRouteContext.Empty.INSTANCE);

        assertEquals(INCoreUiIds.DUNGEON_DIFFICULTY, state.current().orElseThrow().routeId());
        assertTrue(state.backStackSnapshot().isEmpty());
    }

    @Test
    void goBackRestoresPreviousRoute() {
        INCoreUiNavigationState state = new INCoreUiNavigationState();

        state.openRoot(INCoreUiIds.PLAYER_STATUS, INCoreUiRouteContext.Empty.INSTANCE);
        state.pushAndOpen(INCoreUiIds.PLAYER_LEVEL_REWARDS, INCoreUiRouteContext.Empty.INSTANCE);

        assertEquals(INCoreUiIds.PLAYER_STATUS, state.goBack().orElseThrow().routeId());
        assertEquals(INCoreUiIds.PLAYER_STATUS, state.current().orElseThrow().routeId());
    }
}
