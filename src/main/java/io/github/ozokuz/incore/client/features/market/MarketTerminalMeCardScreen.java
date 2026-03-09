package io.github.ozokuz.incore.client.features.market;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.market.content.MarketTerminalMeCardMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MarketTerminalMeCardScreen extends AbstractContainerScreen<MarketTerminalMeCardMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.MACHINE;
    private static final int TEXT_COLOR = THEME.theme().text().secondary();

    public MarketTerminalMeCardScreen(MarketTerminalMeCardMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 188;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);

        ui.drawWindow(x, y, imageWidth, imageHeight);
        drawPanel(guiGraphics, x + 5, y + 5, imageWidth - 10, 14, UIScreenTheme.Machine.HEADER_FILL, UIScreenTheme.Machine.HEADER_BORDER);
        drawPanel(guiGraphics, x + 8, y + 24, imageWidth - 16, 56, UIScreenTheme.Machine.SECTION_FILL, UIScreenTheme.Machine.SECTION_BORDER);
        drawPanel(guiGraphics, x + 8, y + 84, imageWidth - 16, 98, UIScreenTheme.Machine.SECTION_FILL, UIScreenTheme.Machine.SECTION_BORDER);

        drawSlotFrame(guiGraphics, x + MarketTerminalMeCardMenu.CARD_X, y + MarketTerminalMeCardMenu.CARD_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiGraphics,
                        x + MarketTerminalMeCardMenu.PLAYER_INVENTORY_X + col * 18,
                        y + MarketTerminalMeCardMenu.PLAYER_INVENTORY_Y + row * 18
                );
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(
                    guiGraphics,
                    x + MarketTerminalMeCardMenu.PLAYER_INVENTORY_X + col * 18,
                    y + MarketTerminalMeCardMenu.HOTBAR_Y
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 11, 9, UIScreenTheme.Machine.TITLE_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.market.card.slot"), 75, 36, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 12, 88, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        ThemedUi ui = themed(guiGraphics);
        ui.drawRect(x, y, x + width, y + height, fillColor);
        ui.drawBorder(x, y, x + width, y + height, borderColor);
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        themed(guiGraphics).drawSlotFrame(x, y);
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }
}
