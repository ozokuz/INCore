package io.github.ozokuz.incore.client.features.machines;

import io.github.ozokuz.incore.client.ui.UITheme;
import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ResearchScreenRenderer {
    private static final UIScreenTheme THEME = UIScreenTheme.RESEARCH;

    private ResearchScreenRenderer() {
    }

    public static UITheme theme() {
        return THEME.theme();
    }

    public static int titleText() {
        return theme().text().primary();
    }

    public static int primaryText() {
        return theme().text().primary();
    }

    public static int secondaryText() {
        return theme().text().secondary();
    }

    public static int mutedText() {
        return theme().text().muted();
    }

    public static int accentText() {
        return theme().text().accent();
    }

    public static int successText() {
        return theme().text().success();
    }

    public static int warningText() {
        return theme().text().warning();
    }

    public static int dangerText() {
        return theme().text().danger();
    }

    public static ThemedUi ui(GuiGraphics guiGraphics, Font font) {
        return new ThemedUi(guiGraphics, font, THEME.theme());
    }

    public static ThemedUi ui(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }

    public static void drawBackdrop(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        ui(guiGraphics).drawBackdrop(screenWidth, screenHeight);
    }

    public static void drawAccentedWindow(GuiGraphics guiGraphics, int x, int y, int width, int height, int accentColor) {
        drawAccentedFrame(guiGraphics, theme().window(), x, y, width, height, accentColor);
    }

    public static void drawAccentedPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int accentColor) {
        drawAccentedFrame(guiGraphics, theme().panel(), x, y, width, height, accentColor);
    }

    public static void drawAccentedFrame(GuiGraphics guiGraphics, UITheme.Frame frame, int x, int y, int width, int height, int accentColor) {
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

    public static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        ui(guiGraphics).drawSlotFrame(x, y);
    }

    public static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y, int outerColor, int innerColor, int highlightColor) {
        ThemedUi themedUi = ui(guiGraphics);
        themedUi.drawRect(x - 1, y - 1, x + 17, y + 17, outerColor);
        themedUi.drawRect(x, y, x + 16, y + 16, innerColor);
        themedUi.drawRect(x, y, x + 16, y + 1, highlightColor);
        themedUi.drawRect(x, y, x + 1, y + 16, highlightColor);
    }

    public static void drawMachineSlotFrame(GuiGraphics guiGraphics, int x, int y, int accentColor) {
        drawSlotFrame(guiGraphics, x, y, theme().slot().borderTop(), theme().slot().fill(), accentColor);
    }

    public static void drawCompactProgressBar(GuiGraphics guiGraphics, int x, int y, int width, float ratio, int fillColor) {
        drawProgressBar(guiGraphics, x, y, width, 5, ratio, fillColor);
    }

    public static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float ratio) {
        drawProgressBar(guiGraphics, x, y, width, height, ratio, theme().progress().fill());
    }

    public static void drawAlternateProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float ratio) {
        drawProgressBar(guiGraphics, x, y, width, height, ratio, theme().progress().fillAlt());
    }

    public static void drawProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, float ratio, int fillColor) {
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

    public static void drawRowFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        ThemedUi themedUi = ui(guiGraphics);
        themedUi.drawRect(x, y, x + width, y + height, fillColor);
        themedUi.drawBorder(x, y, x + width, y + height, borderColor);
    }

    public static void drawScrollbar(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            float position01,
            float visibleRatio,
            int thumbFill,
            int thumbBorder
    ) {
        ui(guiGraphics).drawScrollbar(
                x,
                y,
                width,
                height,
                position01,
                visibleRatio,
                theme().progress().trackFill(),
                theme().progress().trackBorder(),
                thumbFill,
                thumbBorder
        );
    }

    public static void drawButtonFrame(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor, int accentColor) {
        drawAccentedFrame(
                guiGraphics,
                new UITheme.Frame(fillColor, borderColor, borderColor, borderColor, borderColor),
                x,
                y,
                width,
                height,
                accentColor
        );
    }

    public static void drawCenteredText(GuiGraphics guiGraphics, Font font, Component text, int centerX, int y, int color) {
        ui(guiGraphics, font).drawCenteredText(text, centerX, y, color);
    }

    private static void drawAccentStripe(ThemedUi ui, int x, int y, int width, int accentColor) {
        if (width <= 2) {
            return;
        }
        ui.drawRect(x + 1, y + 1, x + width - 1, y + 3, accentColor);
    }
}
