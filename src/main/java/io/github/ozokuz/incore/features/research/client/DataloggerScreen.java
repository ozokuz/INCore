package io.github.ozokuz.incore.features.research.client;

import io.github.ozokuz.incore.features.research.discovery.DataloggerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class DataloggerScreen extends AbstractContainerScreen<DataloggerMenu> {
    public DataloggerScreen(DataloggerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xFF16130F, 0xFF5A4430);
        drawPanel(guiGraphics, x + 8, y + 18, imageWidth - 16, 54, 0xFF201A15, 0xFF5A4430);
        drawPanel(guiGraphics, x + 8, y + 80, imageWidth - 16, 78, 0xFF1C1713, 0xFF5A4430);

        drawSlot(guiGraphics, x + 80, y + 34);

        int barX = x + 10;
        int barY = y + 72;
        int barW = imageWidth - 20;
        int fill = menu.progressScaled(barW - 2);
        guiGraphics.fill(barX, barY, barX + barW, barY + 6, 0xFF3B2D22);
        guiGraphics.fill(barX + 1, barY + 1, barX + barW - 1, barY + 5, 0xFF14100D);
        if (fill > 0) {
            guiGraphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 5, 0xFFBE8A45);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0xFFF3E6D3, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.datalogger.output"), 70, 20, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.datalogger.progress", menu.progressTicks(), menu.maxProgressTicks()), 10, 52, 0xFFE2D0B7, false);
        String statusKey = menu.getSlot(0).hasItem()
                ? "screen.incore.datalogger.status.ready"
                : "screen.incore.datalogger.status.scanning";
        guiGraphics.drawString(font, Component.translatable(statusKey), 10, 64, 0xFFB7C8D9, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, 86, 0xFFD2BDA2, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        drawPanel(guiGraphics, x - 1, y - 1, 18, 18, 0xFF211914, 0xFFB07C42);
    }
}
