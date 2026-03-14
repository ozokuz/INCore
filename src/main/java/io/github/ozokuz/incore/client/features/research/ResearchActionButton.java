package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.client.features.machines.ResearchScreenRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

final class ResearchActionButton extends AbstractButton {
    private final OnPress onPress;
    private final int accentColor;

    ResearchActionButton(int x, int y, int width, int height, Component message, int accentColor, OnPress onPress) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.accentColor = accentColor;
    }

    @Override
    public void onPress() {
        onPress.onPress(this);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        boolean hovered = isHoveredOrFocused();
        int fill = hovered ? 0xFF1F2835 : ResearchScreenRenderer.theme().panel().fill();
        int border = hovered ? accentColor : ResearchScreenRenderer.theme().panel().borderTop();
        int textColor = hovered ? ResearchScreenRenderer.primaryText() : ResearchScreenRenderer.secondaryText();

        if (!active) {
            fill = 0xFF131922;
            border = 0xFF2B3442;
            textColor = ResearchScreenRenderer.mutedText();
        }

        ResearchScreenRenderer.drawButtonFrame(guiGraphics, getX(), getY(), getWidth(), getHeight(), fill, border, active ? accentColor : border);

        Font font = Minecraft.getInstance().font;
        int textX = getX() + (getWidth() - font.width(getMessage())) / 2;
        int textY = getY() + (getHeight() - font.lineHeight) / 2 + 1;
        guiGraphics.drawString(font, getMessage(), textX, textY, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        defaultButtonNarrationText(narrationElementOutput);
    }

    @FunctionalInterface
    interface OnPress {
        void onPress(ResearchActionButton button);
    }
}
