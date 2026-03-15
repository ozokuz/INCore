package ozokuz.incore.integration.ldlib.ui.elements;

import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.ui.data.FillDirection;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ProgressBar;
import com.lowdragmc.lowdraglib2.gui.ui.rendering.GUIContext;

public final class ClippedTextureProgressBar extends ProgressBar {
    private final IGuiTexture backgroundTexture;
    private final IGuiTexture fillTexture;

    public ClippedTextureProgressBar(IGuiTexture backgroundTexture, IGuiTexture fillTexture) {
        this.backgroundTexture = backgroundTexture;
        this.fillTexture = fillTexture;
        clearAllChildren();
        label.setDisplay(false);
        setOverflowVisible(false);
    }

    @Override
    protected void updateProgressBarStyle(float normalizedValue) {
        // Textured clipped bars render directly in drawBackgroundAdditional instead of resizing a child element.
    }

    @Override
    public void drawBackgroundAdditional(GUIContext guiContext) {
        float x = getPositionX();
        float y = getPositionY();
        float width = getSizeWidth();
        float height = getSizeHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        guiContext.drawTexture(backgroundTexture, x, y, width, height);

        float progress = Math.max(0.0F, Math.min(1.0F, getNormalizedValue()));
        if (progress <= 0.0F) {
            return;
        }

        FillDirection fillDirection = getProgressBarStyle().fillDirection();
        if (fillDirection == FillDirection.ALWAYS_FULL) {
            guiContext.drawTexture(fillTexture, x, y, width, height);
            return;
        }

        float clipX = x;
        float clipY = y;
        float clipWidth = width;
        float clipHeight = height;
        switch (fillDirection) {
            case LEFT_TO_RIGHT -> {
                clipWidth = width * progress;
            }
            case RIGHT_TO_LEFT -> {
                clipWidth = width * progress;
                clipX = x + width - clipWidth;
            }
            case UP_TO_DOWN -> {
                clipHeight = height * progress;
            }
            case DOWN_TO_UP -> {
                clipHeight = height * progress;
                clipY = y + height - clipHeight;
            }
            default -> {
                clipWidth = width;
                clipHeight = height;
            }
        }

        if (clipWidth <= 0 || clipHeight <= 0) {
            return;
        }

        guiContext.graphics.flush();
        guiContext.enableScissor(clipX, clipY, clipWidth, clipHeight);
        guiContext.drawTexture(fillTexture, x, y, width, height);
        guiContext.graphics.flush();
        guiContext.disableScissor();
    }
}
