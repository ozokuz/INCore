package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;

final class BattlePassSurfaceElement extends UIElement {
    enum Kind {
        WINDOW,
        PANEL
    }

    private final Kind kind;

    private BattlePassSurfaceElement(Kind kind) {
        this.kind = kind;
        internalSetup();
    }

    static BattlePassSurfaceElement window() {
        return new BattlePassSurfaceElement(Kind.WINDOW);
    }

    static BattlePassSurfaceElement panel() {
        return new BattlePassSurfaceElement(Kind.PANEL);
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        int x = Math.round(getPositionX());
        int y = Math.round(getPositionY());
        int width = Math.round(getSizeWidth());
        int height = Math.round(getSizeHeight());
        if (width <= 0 || height <= 0) {
            return;
        }

        ThemedUi themedUi = new ThemedUi(guiContext.graphics, UIScreenTheme.BATTLEPASS_TASKS.theme());
        if (this.kind == Kind.WINDOW) {
            themedUi.drawWindow(x, y, width, height);
        } else {
            themedUi.drawPanel(x, y, width, height);
        }
    }
}
