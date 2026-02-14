package io.github.ozokuz.incore.client.status;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class AdvancementWindowRenderer {
    private static final ResourceLocation WINDOW_TEXTURE = ResourceLocation.parse("minecraft:textures/gui/advancements/window.png");
    private static final ResourceLocation INSIDE_TEXTURE = ResourceLocation.parse("minecraft:textures/gui/advancements/backgrounds/stone.png");
    private static final int WINDOW_TEXTURE_SIZE = 256;
    private static final int INSIDE_TILE_SIZE = 16;

    public static final int BASE_WIDTH = 252;
    public static final int BASE_HEIGHT = 140;
    public static final int BORDER_LEFT = 9;
    public static final int BORDER_TOP = 18;
    public static final int BORDER_RIGHT = 9;
    public static final int BORDER_BOTTOM = 9;

    private AdvancementWindowRenderer() {
    }

    public static void draw(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int clampedWidth = Math.max(BORDER_LEFT + BORDER_RIGHT + 1, width);
        int clampedHeight = Math.max(BORDER_TOP + BORDER_BOTTOM + 1, height);

        drawInside(guiGraphics, x, y, clampedWidth, clampedHeight);
        drawFrame(guiGraphics, x, y, clampedWidth, clampedHeight);
    }

    private static void drawInside(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int insideX = x + BORDER_LEFT;
        int insideY = y + BORDER_TOP;
        int insideWidth = width - BORDER_LEFT - BORDER_RIGHT;
        int insideHeight = height - BORDER_TOP - BORDER_BOTTOM;

        for (int tileY = 0; tileY < insideHeight; tileY += INSIDE_TILE_SIZE) {
            for (int tileX = 0; tileX < insideWidth; tileX += INSIDE_TILE_SIZE) {
                int drawWidth = Math.min(INSIDE_TILE_SIZE, insideWidth - tileX);
                int drawHeight = Math.min(INSIDE_TILE_SIZE, insideHeight - tileY);
                guiGraphics.blit(
                        INSIDE_TEXTURE,
                        insideX + tileX,
                        insideY + tileY,
                        0.0F,
                        0.0F,
                        drawWidth,
                        drawHeight,
                        INSIDE_TILE_SIZE,
                        INSIDE_TILE_SIZE
                );
            }
        }
    }

    private static void drawFrame(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        int topSourceWidth = BASE_WIDTH - BORDER_LEFT - BORDER_RIGHT;
        int middleSourceHeight = BASE_HEIGHT - BORDER_TOP - BORDER_BOTTOM;
        int topTargetWidth = width - BORDER_LEFT - BORDER_RIGHT;
        int middleTargetHeight = height - BORDER_TOP - BORDER_BOTTOM;

        // Top-left corner
        guiGraphics.blit(WINDOW_TEXTURE, x, y, 0.0F, 0.0F, BORDER_LEFT, BORDER_TOP, WINDOW_TEXTURE_SIZE, WINDOW_TEXTURE_SIZE);
        // Top-right corner
        guiGraphics.blit(
                WINDOW_TEXTURE,
                x + width - BORDER_RIGHT,
                y,
                (float) (BASE_WIDTH - BORDER_RIGHT),
                0.0F,
                BORDER_RIGHT,
                BORDER_TOP,
                WINDOW_TEXTURE_SIZE,
                WINDOW_TEXTURE_SIZE
        );
        // Bottom-left corner
        guiGraphics.blit(
                WINDOW_TEXTURE,
                x,
                y + height - BORDER_BOTTOM,
                0.0F,
                (float) (BASE_HEIGHT - BORDER_BOTTOM),
                BORDER_LEFT,
                BORDER_BOTTOM,
                WINDOW_TEXTURE_SIZE,
                WINDOW_TEXTURE_SIZE
        );
        // Bottom-right corner
        guiGraphics.blit(
                WINDOW_TEXTURE,
                x + width - BORDER_RIGHT,
                y + height - BORDER_BOTTOM,
                (float) (BASE_WIDTH - BORDER_RIGHT),
                (float) (BASE_HEIGHT - BORDER_BOTTOM),
                BORDER_RIGHT,
                BORDER_BOTTOM,
                WINDOW_TEXTURE_SIZE,
                WINDOW_TEXTURE_SIZE
        );

        // Top edge
        guiGraphics.blit(
                WINDOW_TEXTURE,
                x + BORDER_LEFT,
                y,
                topTargetWidth,
                BORDER_TOP,
                (float) BORDER_LEFT,
                0.0F,
                topSourceWidth,
                BORDER_TOP,
                WINDOW_TEXTURE_SIZE,
                WINDOW_TEXTURE_SIZE
        );
        // Bottom edge
        guiGraphics.blit(
                WINDOW_TEXTURE,
                x + BORDER_LEFT,
                y + height - BORDER_BOTTOM,
                topTargetWidth,
                BORDER_BOTTOM,
                (float) BORDER_LEFT,
                (float) (BASE_HEIGHT - BORDER_BOTTOM),
                topSourceWidth,
                BORDER_BOTTOM,
                WINDOW_TEXTURE_SIZE,
                WINDOW_TEXTURE_SIZE
        );
        // Left edge
        guiGraphics.blit(
                WINDOW_TEXTURE,
                x,
                y + BORDER_TOP,
                BORDER_LEFT,
                middleTargetHeight,
                0.0F,
                (float) BORDER_TOP,
                BORDER_LEFT,
                middleSourceHeight,
                WINDOW_TEXTURE_SIZE,
                WINDOW_TEXTURE_SIZE
        );
        // Right edge
        guiGraphics.blit(
                WINDOW_TEXTURE,
                x + width - BORDER_RIGHT,
                y + BORDER_TOP,
                BORDER_RIGHT,
                middleTargetHeight,
                (float) (BASE_WIDTH - BORDER_RIGHT),
                (float) BORDER_TOP,
                BORDER_RIGHT,
                middleSourceHeight,
                WINDOW_TEXTURE_SIZE,
                WINDOW_TEXTURE_SIZE
        );
    }
}
