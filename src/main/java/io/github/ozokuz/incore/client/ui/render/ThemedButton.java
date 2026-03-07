package io.github.ozokuz.incore.client.ui.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

/**
 * A custom button widget that renders with flat dark-themed colors
 * matching the programmatic UI style used throughout INCore screens.
 */
public class ThemedButton extends AbstractButton {
    private static final int FILL_IDLE = 0xFF1E2430;
    private static final int FILL_HOVER = 0xFF283040;
    private static final int FILL_PRESSED = 0xFF161B24;
    private static final int BORDER_IDLE = 0xFF3A4254;
    private static final int BORDER_HOVER = 0xFF5A7090;
    private static final int TEXT_IDLE = 0xFFCDD3DE;
    private static final int TEXT_HOVER = 0xFFE6EBF4;

    private final OnPress onPress;

    public ThemedButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
    }

    @Override
    public void onPress() {
        this.onPress.onPress(this);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = this.isHoveredOrFocused();
        boolean pressed = hovered && net.minecraft.client.gui.screens.Screen.hasShiftDown()
                || (mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight()
                && org.lwjgl.glfw.GLFW.glfwGetMouseButton(
                Minecraft.getInstance().getWindow().getWindow(),
                org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS);

        int fill = pressed ? FILL_PRESSED : (hovered ? FILL_HOVER : FILL_IDLE);
        int border = hovered ? BORDER_HOVER : BORDER_IDLE;
        int textColor = hovered ? TEXT_HOVER : TEXT_IDLE;

        if (!this.active) {
            fill = 0xFF16191F;
            border = 0xFF2A2F38;
            textColor = 0xFF666C78;
        }

        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();

        guiGraphics.fill(x, y, x + w, y + h, fill);
        // borders
        guiGraphics.fill(x, y, x + w, y + 1, border);
        guiGraphics.fill(x, y + h - 1, x + w, y + h, border);
        guiGraphics.fill(x, y, x + 1, y + h, border);
        guiGraphics.fill(x + w - 1, y, x + w, y + h, border);

        Font font = Minecraft.getInstance().font;
        int textX = x + (w - font.width(getMessage())) / 2;
        int textY = y + (h - font.lineHeight) / 2 + 1;
        guiGraphics.drawString(font, getMessage(), textX, textY, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        this.defaultButtonNarrationText(narrationElementOutput);
    }

    @FunctionalInterface
    public interface OnPress {
        void onPress(ThemedButton button);
    }
}
