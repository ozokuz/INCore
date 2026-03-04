package io.github.ozokuz.incore.client.features.cards;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.cards.DeckStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class CardDeckStationScreen extends AbstractContainerScreen<DeckStationMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.CRAFTING;
    private static final int TEXT_COLOR = UIScreenTheme.Crafting.BODY_TEXT;
    private static final int GUI_FILL = UIScreenTheme.Crafting.WINDOW_FILL;
    private static final int PANEL_FILL = UIScreenTheme.Crafting.PANEL_FILL;
    private static final int PANEL_BORDER = UIScreenTheme.Crafting.PANEL_BORDER;
    private static final int SLOT_FILL = UIScreenTheme.Crafting.SLOT_FILL;
    private static final int SLOT_BORDER = UIScreenTheme.Crafting.SLOT_BORDER;
    private static final int TITLE_COLOR = UIScreenTheme.Crafting.TITLE_TEXT;
    private static final int ACCENT_COLOR = UIScreenTheme.Crafting.ACCENT_TEXT;
    private static final int SUCCESS_COLOR = UIScreenTheme.Crafting.SUCCESS_TEXT;
    private static final int ERROR_COLOR = UIScreenTheme.Crafting.DANGER_TEXT;
    private static final int MUTED_COLOR = UIScreenTheme.Crafting.MUTED_TEXT;
    private static final int WARNING_COLOR = UIScreenTheme.Crafting.WARNING_TEXT;

    private static final int RIGHT_PANEL_X = 206;
    private static final int RIGHT_PANEL_Y = 20;
    private static final int RIGHT_PANEL_W = 176;
    private static final int RIGHT_PANEL_H = 158;
    private static final int INVENTORY_PANEL_X = 8;
    private static final int INVENTORY_PANEL_Y = 142;
    private static final int INVENTORY_PANEL_W = 176;
    private static final int INVENTORY_PANEL_H = 87;

    public CardDeckStationScreen(DeckStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 392;
        this.imageHeight = 236;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);

        ui.drawWindow(x, y, imageWidth, imageHeight);
        drawPanel(guiGraphics, x + 6, y + 6, imageWidth - 12, 12, UIScreenTheme.Crafting.HEADER_FILL, UIScreenTheme.Crafting.HEADER_BORDER);
        drawPanel(guiGraphics, x + 8, y + 20, 186, 114, PANEL_FILL, PANEL_BORDER);
        drawPanel(guiGraphics, x + RIGHT_PANEL_X, y + RIGHT_PANEL_Y, RIGHT_PANEL_W, RIGHT_PANEL_H, PANEL_FILL, PANEL_BORDER);
        drawPanel(guiGraphics, x + INVENTORY_PANEL_X, y + INVENTORY_PANEL_Y, INVENTORY_PANEL_W, INVENTORY_PANEL_H, PANEL_FILL, PANEL_BORDER);

        for (int row = 0; row < DeckStationMenu.GRID_ROWS; row++) {
            for (int col = 0; col < DeckStationMenu.GRID_COLUMNS; col++) {
                if (row == 0 && col >= 2 && col <= 5) {
                    continue;
                }
                int slotX = x + DeckStationMenu.GRID_START_X + col * DeckStationMenu.GRID_STEP_X;
                int slotY = y + DeckStationMenu.GRID_START_Y + row * DeckStationMenu.GRID_STEP_Y;
                drawSlotFrame(guiGraphics, slotX, slotY);
            }
        }

        drawSlotFrame(guiGraphics, x + DeckStationMenu.CORE_X, y + DeckStationMenu.CORE_Y);
        drawSlotFrame(guiGraphics, x + DeckStationMenu.BOX_X, y + DeckStationMenu.BOX_Y);
        drawSlotFrame(guiGraphics, x + DeckStationMenu.OUTPUT_X, y + DeckStationMenu.OUTPUT_Y);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + DeckStationMenu.PLAYER_INV_X + col * 18;
                int slotY = y + DeckStationMenu.PLAYER_INV_Y + row * 18;
                drawSlotFrame(guiGraphics, slotX, slotY);
            }
        }
        for (int col = 0; col < 9; col++) {
            int slotX = x + DeckStationMenu.PLAYER_INV_X + col * 18;
            int slotY = y + DeckStationMenu.HOTBAR_Y;
            drawSlotFrame(guiGraphics, slotX, slotY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 10, 8, TITLE_COLOR, false);
        guiGraphics.drawString(font, "Modules", 10, 22, TEXT_COLOR, false);
        guiGraphics.drawString(font, "Core", DeckStationMenu.CORE_X - 1, DeckStationMenu.CORE_Y + 20, ACCENT_COLOR, false);
        guiGraphics.drawString(font, "Box", DeckStationMenu.BOX_X + 2, DeckStationMenu.BOX_Y + 20, ACCENT_COLOR, false);
        guiGraphics.drawString(font, "Output", DeckStationMenu.OUTPUT_X - 3, DeckStationMenu.OUTPUT_Y + 20, ACCENT_COLOR, false);

        guiGraphics.drawString(font, "Deck Preview", RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 6, TITLE_COLOR, false);

        int used = menu.usedPoints();
        int capacity = menu.capacity();
        int modules = menu.moduleCount();
        int maxIntegrity = menu.maxIntegrity();
        boolean overLimit = capacity > 0 && used > capacity;
        int pointsColor = capacity <= 0 ? MUTED_COLOR : (overLimit ? ERROR_COLOR : SUCCESS_COLOR);

        guiGraphics.drawString(font, "Modules: " + modules, RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 20, TEXT_COLOR, false);
        guiGraphics.drawString(
                font,
                "Points: " + used + "/" + capacity,
                RIGHT_PANEL_X + 6,
                RIGHT_PANEL_Y + 32,
                pointsColor,
                false
        );
        guiGraphics.drawString(font, "Max Integrity: " + maxIntegrity, RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 44, TEXT_COLOR, false);

        if (menu.validPreview()) {
            guiGraphics.drawString(font, "Status: Valid", RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 56, SUCCESS_COLOR, false);
        } else {
            String reason = Component.translatable(menu.failureKey().isBlank() ? "incore.cards.deck.missing_core" : menu.failureKey()).getString();
            guiGraphics.drawString(font, "Status: Invalid", RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 56, ERROR_COLOR, false);
            drawWrapped(guiGraphics, reason, RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 68, RIGHT_PANEL_W - 12, 2, UIScreenTheme.Crafting.REASON_TEXT);
        }

        guiGraphics.drawString(font, "Modifiers", RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 92, UIScreenTheme.Crafting.MODIFIER_LABEL_TEXT, false);
        List<String> lines = menu.previewModifierLines();
        if (lines.isEmpty()) {
            guiGraphics.drawString(font, "No active modifiers.", RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 104, MUTED_COLOR, false);
        } else {
            for (int i = 0; i < Math.min(5, lines.size()); i++) {
                String line = lines.get(i);
                int color = line.startsWith("-")
                        ? ERROR_COLOR
                        : (line.startsWith("Undecrypted Cryptics:") ? WARNING_COLOR : SUCCESS_COLOR);
                guiGraphics.drawString(font, line, RIGHT_PANEL_X + 6, RIGHT_PANEL_Y + 104 + i * 10, color, false);
            }
        }

        guiGraphics.drawString(font, playerInventoryTitle, INVENTORY_PANEL_X + 4, INVENTORY_PANEL_Y + 5, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawWrapped(GuiGraphics guiGraphics, String text, int x, int y, int width, int maxLines, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(text), Math.max(0, width));
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + i * font.lineHeight, color);
        }
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
