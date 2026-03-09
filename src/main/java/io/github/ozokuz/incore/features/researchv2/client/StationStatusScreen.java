package io.github.ozokuz.incore.features.researchv2.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class StationStatusScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected StationStatusScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 122;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        drawPanel(guiGraphics, left, top, imageWidth, imageHeight, panelFillColor(), borderColor(), accentColor());
        drawPanel(guiGraphics, left + 8, top + 20, imageWidth - 16, imageHeight - 28, innerPanelFillColor(), borderColor(), accentColor());
        renderStatusBody(guiGraphics, left, top, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 12, 8, titleColor(), false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected int accentColor() {
        return 0xFF4D8B9D;
    }

    protected int panelFillColor() {
        return 0xFF11161B;
    }

    protected int innerPanelFillColor() {
        return 0xFF171E25;
    }

    protected int borderColor() {
        return 0xFF2B3742;
    }

    protected int titleColor() {
        return 0xFFF2F6FA;
    }

    protected int labelColor() {
        return 0xFFBFD0DE;
    }

    protected int valueColor() {
        return 0xFFE6EEF5;
    }

    protected int okColor() {
        return 0xFF8FDF8A;
    }

    protected int warnColor() {
        return 0xFFE4B36B;
    }

    protected abstract void renderStatusBody(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY);

    protected void drawKeyValue(GuiGraphics guiGraphics, int x, int y, Component label, Component value, int color) {
        guiGraphics.drawString(font, label, x, y, labelColor(), false);
        guiGraphics.drawString(font, value, x + 78, y, color, false);
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor, int accentColor) {
        guiGraphics.fill(x, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 3, accentColor);
        guiGraphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, 0x66000000);
    }
}
