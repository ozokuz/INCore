package ozokuz.incore.integration.ldlib.ui.elements;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;

public final class INCoreInfoSurfaceElement extends UIElement {
    public enum Kind {
        BACKDROP,
        WINDOW,
        CARD
    }

    private final Kind kind;

    private INCoreInfoSurfaceElement(Kind kind) {
        this.kind = kind;
        internalSetup();
    }

    public static INCoreInfoSurfaceElement backdrop() {
        return new INCoreInfoSurfaceElement(Kind.BACKDROP);
    }

    public static INCoreInfoSurfaceElement window() {
        return new INCoreInfoSurfaceElement(Kind.WINDOW);
    }

    public static INCoreInfoSurfaceElement card() {
        return new INCoreInfoSurfaceElement(Kind.CARD);
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

        if (this.kind == Kind.BACKDROP) {
            guiContext.graphics.fillGradient(
                    x,
                    y,
                    x + width,
                    y + height,
                    UIScreenTheme.INFO.theme().backdrop().top(),
                    UIScreenTheme.INFO.theme().backdrop().bottom()
            );
            return;
        }

        ThemedUi themedUi = new ThemedUi(guiContext.graphics, UIScreenTheme.INFO.theme());
        if (this.kind == Kind.WINDOW) {
            themedUi.drawWindow(x, y, width, height);
        } else {
            themedUi.drawCard(x, y, width, height);
        }
    }
}
