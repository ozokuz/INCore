package io.github.ozokuz.incore.client.features.research;

import io.github.ozokuz.incore.features.research.discovery.TranslatorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class TranslatorScreen extends AbstractContainerScreen<TranslatorMenu> {
    public TranslatorScreen(TranslatorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xFF141110, 0xFF4D6178);
        drawPanel(guiGraphics, x + 8, y + 18, imageWidth - 16, 54, 0xFF1C2028, 0xFF4D6178);
        drawPanel(guiGraphics, x + 8, y + 80, imageWidth - 16, 78, 0xFF171C23, 0xFF4D6178);

        drawSlot(guiGraphics, x + 44, y + 34);
        drawSlot(guiGraphics, x + 116, y + 34);

        int barX = x + 10;
        int barY = y + 72;
        int barW = imageWidth - 20;
        int fill = menu.progressScaled(barW - 2);
        guiGraphics.fill(barX, barY, barX + barW, barY + 6, 0xFF243143);
        guiGraphics.fill(barX + 1, barY + 1, barX + barW - 1, barY + 5, 0xFF101722);
        if (fill > 0) {
            guiGraphics.fill(barX + 1, barY + 1, barX + 1 + fill, barY + 5, 0xFF55A9E6);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 8, 6, 0xFFF3E6D3, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.translator.input"), 35, 20, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.translator.output"), 108, 20, 0xFFD2BDA2, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.translator.progress", menu.progressTicks(), menu.maxProgressTicks()), 10, 52, 0xFFCBDBF0, false);
        String statusKey = menu.getSlot(1).hasItem()
                ? "screen.incore.translator.status.ready"
                : (menu.getSlot(0).hasItem() ? "screen.incore.translator.status.decoding" : "screen.incore.translator.status.idle");
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
        drawPanel(guiGraphics, x - 1, y - 1, 18, 18, 0xFF181F2A, 0xFF46566F);
    }
}
