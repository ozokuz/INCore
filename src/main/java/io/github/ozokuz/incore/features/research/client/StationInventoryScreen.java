package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.client.ui.UITheme;
import io.github.ozokuz.incore.features.research.station.AbstractStationInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class StationInventoryScreen<T extends AbstractStationInventoryMenu> extends AbstractContainerScreen<T> {
    private static final UITheme THEME = ResearchScreenRenderer.theme();
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

        ResearchScreenRenderer.drawAccentedWindow(guiGraphics, left, top, imageWidth, imageHeight, accentColor());
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, machineLeft, machineTop, machineWidth, machineHeight, accentColor());
        ResearchScreenRenderer.drawAccentedPanel(guiGraphics, left + 4, playerTop, imageWidth - 8, playerHeight, accentColor());

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (!slot.isActive()) {
                continue;
            }
            if (slotIndex < menu.machineSlotCount()) {
                ResearchScreenRenderer.drawMachineSlotFrame(guiGraphics, left + slot.x, top + slot.y, accentColor());
                continue;
            }
            ResearchScreenRenderer.drawSlotFrame(guiGraphics, left + slot.x, top + slot.y);
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
        ResearchScreenRenderer.drawBackdrop(guiGraphics, width, height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    protected int accentColor() {
        return 0xFFB8742D;
    }

    protected int panelFillColor() {
        return THEME.window().fill();
    }

    protected int machineFillColor() {
        return THEME.panel().fill();
    }

    protected int inventoryFillColor() {
        return THEME.panel().fill();
    }

    protected int borderColor() {
        return THEME.window().borderTop();
    }

    protected int titleColor() {
        return THEME.text().primary();
    }

    protected int subtitleColor() {
        return THEME.text().secondary();
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
}
