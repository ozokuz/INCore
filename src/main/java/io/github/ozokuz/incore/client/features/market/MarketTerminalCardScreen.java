package io.github.ozokuz.incore.client.features.market;

import io.github.ozokuz.incore.features.market.content.MarketTerminalCardMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MarketTerminalCardScreen extends AbstractContainerScreen<MarketTerminalCardMenu> {
    private static final int TEXT_COLOR = 0xCDD3DE;

    public MarketTerminalCardScreen(MarketTerminalCardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 188;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xFF13161A, 0xFF4A4F5A);
        drawPanel(guiGraphics, x + 5, y + 5, imageWidth - 10, 14, 0xFF20252C, 0xFF3D4350);
        drawPanel(guiGraphics, x + 8, y + 24, imageWidth - 16, 68, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + 8, y + 94, imageWidth - 16, 86, 0xFF1A1F26, 0xFF363D49);

        drawSlotFrame(guiGraphics, x + MarketTerminalCardMenu.CARD_X, y + MarketTerminalCardMenu.CARD_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiGraphics,
                        x + MarketTerminalCardMenu.PLAYER_INVENTORY_X + col * 18,
                        y + MarketTerminalCardMenu.PLAYER_INVENTORY_Y + row * 18
                );
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(
                    guiGraphics,
                    x + MarketTerminalCardMenu.PLAYER_INVENTORY_X + col * 18,
                    y + MarketTerminalCardMenu.HOTBAR_Y
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 11, 9, 0xE6EBF4, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.market.card.slot"), 12, 50, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 12, 100, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        drawPanel(guiGraphics, x - 1, y - 1, 18, 18, 0xFF252A32, 0xFF4A5261);
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF181D24);
    }
}
