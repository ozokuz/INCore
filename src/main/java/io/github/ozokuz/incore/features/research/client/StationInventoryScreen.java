package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.features.research.station.AbstractStationInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class StationInventoryScreen<T extends AbstractStationInventoryMenu> extends AbstractContainerScreen<T> {
    private final int rows;

    public StationInventoryScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.rows = Math.max(1, menu.machineRows());
        this.imageHeight = 114 + (rows * 18);
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 12;
        this.titleLabelY = titleLabelY();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        int machineLeft = left + menu.machineSectionLeft() - 4;
        int machineTop = top + machineSectionTopOffset();
        int machineWidth = menu.machineSectionWidth() + 8;
        int machineHeight = (rows * 18) + 26;
        int playerTop = top + playerSectionTopOffset();
        int playerHeight = 92;

        drawPanel(guiGraphics, left, top, imageWidth, imageHeight, panelFillColor(), borderColor(), accentColor());
        drawPanel(guiGraphics, machineLeft, machineTop, machineWidth, machineHeight, machineFillColor(), borderColor(), accentColor());
        drawPanel(guiGraphics, left + 4, playerTop, imageWidth - 8, playerHeight, inventoryFillColor(), borderColor(), accentColor());

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (!slot.isActive()) {
                continue;
            }
            drawSlotWell(guiGraphics, left + slot.x, top + slot.y, slotIndex < menu.machineSlotCount());
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, titleColor(), false);
        renderSubtitle(guiGraphics);
        guiGraphics.drawString(font, playerInventoryTitle, 12, inventoryLabelY, subtitleColor(), false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected int accentColor() {
        return 0xFFB8742D;
    }

    protected int panelFillColor() {
        return 0xFF13100F;
    }

    protected int machineFillColor() {
        return 0xFF1A1614;
    }

    protected int inventoryFillColor() {
        return 0xFF171312;
    }

    protected int borderColor() {
        return 0xFF2F2722;
    }

    protected int titleColor() {
        return 0xFFF3E6D3;
    }

    protected int subtitleColor() {
        return 0xFFD2BDA2;
    }

    protected int titleLabelY() {
        return 9;
    }

    protected int machineSectionTopOffset() {
        return 4;
    }

    protected int playerSectionTopOffset() {
        return 18 + rows * 18;
    }

    protected void renderSubtitle(GuiGraphics guiGraphics) {
    }

    private void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor, int accentColor) {
        guiGraphics.fill(x, y, x + width, y + height, borderColor);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fillColor);
        guiGraphics.fill(x + 1, y + 1, x + width - 1, y + 3, accentColor);
        guiGraphics.fill(x + 2, y + height - 2, x + width - 2, y + height - 1, 0x66000000);
    }

    private void drawSlotWell(GuiGraphics guiGraphics, int x, int y, boolean machineSlot) {
        int outer = machineSlot ? 0xFF4B3322 : 0xFF3A312B;
        int inner = machineSlot ? 0xFF211914 : 0xFF1B1714;
        int highlight = machineSlot ? accentColor() : 0xFF7F6A5C;
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, outer);
        guiGraphics.fill(x, y, x + 16, y + 16, inner);
        guiGraphics.fill(x, y, x + 16, y + 1, highlight);
        guiGraphics.fill(x, y, x + 1, y + 16, highlight);
    }
}
