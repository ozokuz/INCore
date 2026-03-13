package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.client.ui.UITheme;
import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class ResearchScreenRenderer {
    private static final UIScreenTheme THEME = UIScreenTheme.RESEARCH;

    private ResearchScreenRenderer() {
    }

    static UITheme theme() {
        return THEME.theme();
    }

    static ThemedUi ui(GuiGraphics guiGraphics, Font font) {
        return new ThemedUi(guiGraphics, font, THEME.theme());
    }

    static ThemedUi ui(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }

    static void drawBackdrop(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        ui(guiGraphics).drawBackdrop(screenWidth, screenHeight);
    }

    static void drawAccentedWindow(GuiGraphics guiGraphics, int x, int y, int width, int height, int accentColor) {
        ThemedUi themedUi = ui(guiGraphics);
        themedUi.drawWindow(x, y, width, height);
        drawAccentStripe(themedUi, x, y, width, accentColor);
    }

    static void drawAccentedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int accentColor) {
        ThemedUi themedUi = ui(guiGraphics);
        themedUi.drawPanel(x, y, width, height);
        drawAccentStripe(themedUi, x, y, width, accentColor);
    }

    static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        ui(guiGraphics).drawSlotFrame(x, y);
    }

    static void drawMachineSlotFrame(GuiGraphics guiGraphics, int x, int y, int accentColor) {
        ThemedUi themedUi = ui(guiGraphics);
        themedUi.drawSlotFrame(x, y);
        themedUi.drawRect(x, y, x + 16, y + 1, accentColor);
        themedUi.drawRect(x, y, x + 1, y + 16, accentColor);
    }

    static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float ratio) {
        drawProgressBar(guiGraphics, x, y, width, height, ratio, theme().progress().fill());
    }

    static void drawAlternateProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float ratio) {
        drawProgressBar(guiGraphics, x, y, width, height, ratio, theme().progress().fillAlt());
    }

    static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float ratio, int fillColor) {
        ThemedUi themedUi = ui(guiGraphics);
        int clampedWidth = Math.max(3, width);
        int clampedHeight = Math.max(3, height);
        int innerRight = x + clampedWidth - 1;
        int innerBottom = y + clampedHeight - 1;

        themedUi.drawRect(x, y, x + clampedWidth, y + clampedHeight, theme().progress().trackBorder());
        themedUi.drawRect(x + 1, y + 1, innerRight, innerBottom, theme().progress().trackFill());

        int innerWidth = Math.max(1, clampedWidth - 2);
        int filled = Math.round(innerWidth * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled > 0) {
            themedUi.drawRect(x + 1, y + 1, x + 1 + Math.min(innerWidth, filled), innerBottom, fillColor);
        }
    }

    private static void drawAccentStripe(ThemedUi ui, int x, int y, int width, int accentColor) {
        if (width <= 2) {
            return;
        }
        ui.drawRect(x + 1, y + 1, x + width - 1, y + 3, accentColor);
    }
}
