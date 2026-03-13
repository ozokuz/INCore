package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.client.ui.UITheme;
import io.github.ozokuz.incore.features.research.discovery.DataloggerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class DataloggerScreen extends AbstractContainerScreen<DataloggerMenu> {
    private static final int ACCENT_COLOR = 0xFFB07C42;
    private static final UITheme.Frame WINDOW_FRAME = new UITheme.Frame(0xFF16130F, 0xFF5A4430, 0xFF5A4430, 0xFF5A4430, 0xFF5A4430);
    private static final UITheme.Frame PANEL_FRAME = new UITheme.Frame(0xFF201A15, 0xFF5A4430, 0xFF5A4430, 0xFF5A4430, 0xFF5A4430);
    private static final UITheme.Frame LOWER_PANEL_FRAME = new UITheme.Frame(0xFF1C1713, 0xFF5A4430, 0xFF5A4430, 0xFF5A4430, 0xFF5A4430);
    private static final int SLOT_OUTER = 0xFF2E241C;
    private static final int SLOT_INNER = 0xFF211914;
    private static final int BAR_FILL = 0xFFBE8A45;
    private static final int PLAYER_SLOT_OUTER = 0xFF3A312B;
    private static final int PLAYER_SLOT_INNER = 0xFF1B1714;
    private static final int PLAYER_SLOT_HIGHLIGHT = 0xFF7F6A5C;

    public DataloggerScreen(DataloggerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        ResearchScreenRenderer.drawAccentedFrame(guiGraphics, WINDOW_FRAME, x, y, imageWidth, imageHeight, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedFrame(guiGraphics, PANEL_FRAME, x + 8, y + 18, imageWidth - 16, 54, ACCENT_COLOR);
        ResearchScreenRenderer.drawAccentedFrame(guiGraphics, LOWER_PANEL_FRAME, x + 8, y + 80, imageWidth - 16, 78, ACCENT_COLOR);

        ResearchScreenRenderer.drawSlotFrame(guiGraphics, x + 80, y + 34, SLOT_OUTER, SLOT_INNER, ACCENT_COLOR);
        drawPlayerInventorySlots(guiGraphics, x, y);

        int barX = x + 10;
        int barY = y + 72;
        int barW = imageWidth - 20;
        int fill = menu.progressScaled(barW - 2);
        ResearchScreenRenderer.drawProgressBar(guiGraphics, barX, barY, barW, 6, fill / (float) (barW - 2), BAR_FILL);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0xFFF3E6D3, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.datalogger.output"), 70, 20, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.datalogger.progress", menu.progressTicks(), menu.maxProgressTicks()), 10, 52, 0xFFE2D0B7, false);
        String statusKey = menu.getSlot(0).hasItem()
                ? "screen.incore.datalogger.status.ready"
                : "screen.incore.datalogger.status.scanning";
        guiGraphics.drawString(font, Component.translatable(statusKey), 10, 64, ResearchScreenRenderer.secondaryText(), false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, 86, 0xFFD2BDA2, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResearchScreenRenderer.drawBackdrop(guiGraphics, width, height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawPlayerInventorySlots(GuiGraphics guiGraphics, int left, int top) {
        for (int slotIndex = 1; slotIndex < menu.slots.size(); slotIndex++) {
            Slot slot = menu.slots.get(slotIndex);
            if (!slot.isActive()) {
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
}
