package io.github.ozokuz.incore.client.features.machines;

import io.github.ozokuz.incore.client.ui.UITheme;
import io.github.ozokuz.incore.features.machines.multiblock.AbstractMachineInventoryMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class MachineInventoryScreen<T extends AbstractMachineInventoryMenu> extends AbstractContainerScreen<T> {
    private static final UITheme THEME = ResearchScreenRenderer.theme();
    private static final UITheme.Frame WINDOW_FRAME = new UITheme.Frame(0xFF13100F, 0xFF2F2722, 0xFF2F2722, 0xFF2F2722, 0xFF2F2722);
    private static final UITheme.Frame MACHINE_FRAME = new UITheme.Frame(0xFF1A1614, 0xFF2F2722, 0xFF2F2722, 0xFF2F2722, 0xFF2F2722);
    private static final UITheme.Frame INVENTORY_FRAME = new UITheme.Frame(0xFF171312, 0xFF2F2722, 0xFF2F2722, 0xFF2F2722, 0xFF2F2722);
    private static final int MACHINE_SLOT_OUTER = 0xFF4B3322;
    private static final int MACHINE_SLOT_INNER = 0xFF211914;
    private static final int PLAYER_SLOT_OUTER = 0xFF3A312B;
    private static final int PLAYER_SLOT_INNER = 0xFF1B1714;
    private static final int PLAYER_SLOT_HIGHLIGHT = 0xFF7F6A5C;
    protected final int rows;
    protected final int slotStartY;

    public MachineInventoryScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.rows = Math.max(1, menu.machineRows());
        this.slotStartY = menu.machineSlotStartY();
        this.imageHeight = slotStartY + rows * 18 + 100;
        this.inventoryLabelY = slotStartY + rows * 18 + 6;
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
        int machineHeight = (rows * 18) + 16;
        int playerTop = top + playerSectionTopOffset();
        int playerHeight = 92;

        ResearchScreenRenderer.drawAccentedFrame(guiGraphics, WINDOW_FRAME, left, top, imageWidth, imageHeight, accentColor());
        ResearchScreenRenderer.drawAccentedFrame(guiGraphics, MACHINE_FRAME, machineLeft, machineTop, machineWidth, machineHeight, accentColor());
        ResearchScreenRenderer.drawAccentedFrame(guiGraphics, INVENTORY_FRAME, left + 4, playerTop, imageWidth - 8, playerHeight, accentColor());

        for (int slotIndex = 0; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (!slot.isActive()) {
                continue;
            }
            if (slotIndex < menu.machineSlotCount()) {
                ResearchScreenRenderer.drawSlotFrame(
                        guiGraphics,
                        left + slot.x,
                        top + slot.y,
                        MACHINE_SLOT_OUTER,
                        MACHINE_SLOT_INNER,
                        accentColor()
                );
                continue;
            }
            ResearchScreenRenderer.drawSlotFrame(
                    guiGraphics,
                    left + slot.x,
                    top + slot.y,
                    PLAYER_SLOT_OUTER,
                    PLAYER_SLOT_INNER,
                    PLAYER_SLOT_HIGHLIGHT
            );
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
        return WINDOW_FRAME.fill();
    }

    protected int machineFillColor() {
        return MACHINE_FRAME.fill();
    }

    protected int inventoryFillColor() {
        return INVENTORY_FRAME.fill();
    }

    protected int borderColor() {
        return WINDOW_FRAME.borderTop();
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
        return slotStartY - 14;
    }

    protected int playerSectionTopOffset() {
        return slotStartY + 4 + rows * 18;
    }

    protected void renderSubtitle(GuiGraphics guiGraphics) {
    }
}
