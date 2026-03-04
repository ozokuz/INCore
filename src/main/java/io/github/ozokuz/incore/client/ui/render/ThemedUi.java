package io.github.ozokuz.incore.client.ui.render;

import io.github.ozokuz.incore.client.ui.UITheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

public final class ThemedUi {
    private final GuiGraphics guiGraphics;
    private final Font font;
    private final UITheme theme;

    public ThemedUi(GuiGraphics guiGraphics, Font font, UITheme theme) {
        this.guiGraphics = guiGraphics;
        this.font = font;
        this.theme = theme;
    }

    public ThemedUi(GuiGraphics guiGraphics, UITheme theme) {
        this.guiGraphics = guiGraphics;
        this.font = null;
        this.theme = theme;
    }

    public UITheme theme() {
        return theme;
    }

    public void drawBackdrop(int screenWidth, int screenHeight) {
        if (theme.backdrop().gradient()) {
            guiGraphics.fillGradient(0, 0, screenWidth, screenHeight, theme.backdrop().top(), theme.backdrop().bottom());
            return;
        }
        guiGraphics.fill(0, 0, screenWidth, screenHeight, theme.backdrop().top());
    }

    public void drawWindow(int x, int y, int width, int height) {
        drawFrame(theme.window(), x, y, width, height);
    }

    public void drawPanel(int x, int y, int width, int height) {
        drawFrame(theme.panel(), x, y, width, height);
    }

    public void drawCard(int x, int y, int width, int height) {
        drawFrame(theme.card(), x, y, width, height);
    }

    public void drawSlotFrame(int x, int y) {
        drawFrame(theme.slot(), x - 1, y - 1, 18, 18);
        guiGraphics.fill(x, y, x + 16, y + 16, theme.slot().fill());
    }

    public void drawRect(int left, int top, int right, int bottom, int color) {
        guiGraphics.fill(left, top, right, bottom, color);
    }

    public void drawBorder(int left, int top, int right, int bottom, int color) {
        if (right - left <= 0 || bottom - top <= 0) {
            return;
        }
        guiGraphics.fill(left, top, right, top + 1, color);
        guiGraphics.fill(left, bottom - 1, right, bottom, color);
        guiGraphics.fill(left, top, left + 1, bottom, color);
        guiGraphics.fill(right - 1, top, right, bottom, color);
    }

    public void drawText(Component text, int x, int y, int color, boolean shadow) {
        guiGraphics.drawString(requireFont(), text, x, y, color, shadow);
    }

    public void drawCenteredText(Component text, int centerX, int y, int color) {
        guiGraphics.drawCenteredString(requireFont(), text, centerX, y, color);
    }

    public void drawChipLeft(int x, int y, Component text, int fillColor, int textColor) {
        int width = requireFont().width(text) + 10;
        guiGraphics.fill(x, y, x + width, y + 11, fillColor);
        guiGraphics.drawString(requireFont(), text, x + 5, y + 2, textColor, false);
    }

    public void drawChipCentered(int centerX, int y, Component text, int fillColor, int textColor) {
        int width = requireFont().width(text) + 14;
        int left = centerX - width / 2;
        guiGraphics.fill(left, y, left + width, y + 12, fillColor);
        guiGraphics.drawCenteredString(requireFont(), text, centerX, y + 2, textColor);
    }

    public void drawScaledItemLine(ItemStack stack, String text, int x, int y, float scale, int textColor) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);

        int textX = 0;
        if (!stack.isEmpty()) {
            guiGraphics.renderItem(stack, 0, 0);
            textX = 20;
        }

        guiGraphics.drawString(requireFont(), Component.literal(text), textX, 4, textColor, false);
        guiGraphics.pose().popPose();
    }

    public void drawProgressBar(int x, int y, int width, int height, float ratio, int trackColor, int fillColor, int borderColor) {
        int clampedWidth = Math.max(1, width);
        int clampedHeight = Math.max(1, height);
        guiGraphics.fill(x, y, x + clampedWidth, y + clampedHeight, trackColor);
        int filled = Math.round(clampedWidth * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled > 0) {
            guiGraphics.fill(x, y, x + filled, y + clampedHeight, fillColor);
        }
        drawBorder(x, y, x + clampedWidth, y + clampedHeight, borderColor);
    }

    public void drawSpriteProgressBar(ResourceLocation bg, ResourceLocation fg, int x, int y, int width, int height, float ratio) {
        int clampedWidth = Math.max(1, width);
        int clampedHeight = Math.max(1, height);
        guiGraphics.blitSprite(bg, x, y, clampedWidth, clampedHeight);

        int filled = Math.round(clampedWidth * Mth.clamp(ratio, 0.0F, 1.0F));
        if (filled <= 0) {
            return;
        }

        guiGraphics.enableScissor(x, y, x + filled, y + clampedHeight);
        guiGraphics.blitSprite(fg, x, y, clampedWidth, clampedHeight);
        guiGraphics.disableScissor();
    }

    public void drawScrollbar(
            int x,
            int y,
            int width,
            int height,
            float position01,
            float visibleRatio,
            int trackFill,
            int trackBorder,
            int thumbFill,
            int thumbBorder
    ) {
        int clampedWidth = Math.max(1, width);
        int clampedHeight = Math.max(1, height);
        guiGraphics.fill(x, y, x + clampedWidth, y + clampedHeight, trackFill);
        drawBorder(x, y, x + clampedWidth, y + clampedHeight, trackBorder);

        if (visibleRatio >= 1.0F) {
            return;
        }

        int thumbHeight = Math.max(4, Math.min(clampedHeight, Math.round(clampedHeight * Mth.clamp(visibleRatio, 0.0F, 1.0F))));
        int travel = Math.max(0, clampedHeight - thumbHeight);
        int thumbTop = y + Math.round(Mth.clamp(position01, 0.0F, 1.0F) * travel);
        guiGraphics.fill(x + 1, thumbTop, x + clampedWidth - 1, thumbTop + thumbHeight, thumbFill);
        drawBorder(x + 1, thumbTop, x + clampedWidth - 1, thumbTop + thumbHeight, thumbBorder);
    }

    private void drawFrame(UITheme.Frame frame, int x, int y, int width, int height) {
        int right = x + Math.max(0, width);
        int bottom = y + Math.max(0, height);
        guiGraphics.fill(x, y, right, bottom, frame.fill());
        guiGraphics.fill(x, y, right, y + 1, frame.borderTop());
        guiGraphics.fill(x, bottom - 1, right, bottom, frame.borderBottom());
        guiGraphics.fill(x, y, x + 1, bottom, frame.borderLeft());
        guiGraphics.fill(right - 1, y, right, bottom, frame.borderRight());
    }

    private Font requireFont() {
        return Objects.requireNonNull(this.font, "Font is required for text rendering");
    }
}
