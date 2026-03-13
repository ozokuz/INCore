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
        drawAccentedFrame(guiGraphics, theme().window(), x, y, width, height, accentColor);
    }

    static void drawAccentedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int accentColor) {
        drawAccentedFrame(guiGraphics, theme().panel(), x, y, width, height, accentColor);
    }

    static void drawAccentedFrame(GuiGraphics guiGraphics, UITheme.Frame frame, int x, int y, int width, int height, int accentColor) {
        ThemedUi themedUi = ui(guiGraphics);
        int right = x + Math.max(0, width);
        int bottom = y + Math.max(0, height);
        themedUi.drawRect(x, y, right, bottom, frame.fill());
        themedUi.drawRect(x, y, right, y + 1, frame.borderTop());
        themedUi.drawRect(x, bottom - 1, right, bottom, frame.borderBottom());
        themedUi.drawRect(x, y, x + 1, bottom, frame.borderLeft());
        themedUi.drawRect(right - 1, y, right, bottom, frame.borderRight());
        drawAccentStripe(themedUi, x, y, width, accentColor);
    }

    static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        ui(guiGraphics).drawSlotFrame(x, y);
    }

    static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y, int outerColor, int innerColor, int highlightColor) {
        ThemedUi themedUi = ui(guiGraphics);
        themedUi.drawRect(x - 1, y - 1, x + 17, y + 17, outerColor);
        themedUi.drawRect(x, y, x + 16, y + 16, innerColor);
        themedUi.drawRect(x, y, x + 16, y + 1, highlightColor);
        themedUi.drawRect(x, y, x + 1, y + 16, highlightColor);
    }

    static void drawMachineSlotFrame(GuiGraphics guiGraphics, int x, int y, int accentColor) {
        drawSlotFrame(guiGraphics, x, y, theme().slot().borderTop(), theme().slot().fill(), accentColor);
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
