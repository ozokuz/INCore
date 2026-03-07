package io.github.ozokuz.incore.client.features.cards;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.cards.DeckStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class CardDeckStationScreen extends AbstractContainerScreen<DeckStationMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.CRAFTING;

    private static final int TITLE_COLOR = UIScreenTheme.Crafting.TITLE_TEXT;
    private static final int TEXT_COLOR = UIScreenTheme.Crafting.BODY_TEXT;
    private static final int ACCENT_COLOR = UIScreenTheme.Crafting.ACCENT_TEXT;
    private static final int SUCCESS_COLOR = UIScreenTheme.Crafting.SUCCESS_TEXT;
    private static final int ERROR_COLOR = UIScreenTheme.Crafting.DANGER_TEXT;
    private static final int MUTED_COLOR = UIScreenTheme.Crafting.MUTED_TEXT;
    private static final int WARNING_COLOR = UIScreenTheme.Crafting.WARNING_TEXT;

    // Panel layout
    private static final int HEADER_Y = 6;
    private static final int HEADER_H = 14;

    private static final int MODULE_PANEL_X = 8;
    private static final int MODULE_PANEL_Y = 22;
    private static final int MODULE_PANEL_W = 186;
    private static final int MODULE_PANEL_H = 118;

    private static final int RIGHT_PANEL_X = 200;
    private static final int RIGHT_PANEL_Y = 22;
    private static final int RIGHT_PANEL_W = 184;
    private static final int RIGHT_PANEL_H = 118;

    private static final int INVENTORY_PANEL_X = 8;
    private static final int INVENTORY_PANEL_Y = 144;
    private static final int INVENTORY_PANEL_W = 176;
    private static final int INVENTORY_PANEL_H = 87;

    public CardDeckStationScreen(DeckStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 392;
        this.imageHeight = 238;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);

        // Window
        ui.drawWindow(x, y, imageWidth, imageHeight);

        // Header bar
        drawPanel(guiGraphics, x + 5, y + HEADER_Y, imageWidth - 10, HEADER_H,
                UIScreenTheme.Crafting.HEADER_FILL, UIScreenTheme.Crafting.HEADER_BORDER);

        // Module grid panel
        drawPanel(guiGraphics, x + MODULE_PANEL_X, y + MODULE_PANEL_Y, MODULE_PANEL_W, MODULE_PANEL_H,
                UIScreenTheme.Crafting.PANEL_FILL, UIScreenTheme.Crafting.PANEL_BORDER);

        // Right preview panel
        drawPanel(guiGraphics, x + RIGHT_PANEL_X, y + RIGHT_PANEL_Y, RIGHT_PANEL_W, RIGHT_PANEL_H,
                UIScreenTheme.Crafting.PANEL_FILL, UIScreenTheme.Crafting.PANEL_BORDER);

        // Inventory panel
        drawPanel(guiGraphics, x + INVENTORY_PANEL_X, y + INVENTORY_PANEL_Y, INVENTORY_PANEL_W, INVENTORY_PANEL_H,
                UIScreenTheme.Crafting.PANEL_FILL, UIScreenTheme.Crafting.PANEL_BORDER);

        // Module grid slot frames (skip row 0 cols 2-5 as those are Core/Box)
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

        // Core and Box slot frames
        drawSlotFrame(guiGraphics, x + DeckStationMenu.CORE_X, y + DeckStationMenu.CORE_Y);
        drawSlotFrame(guiGraphics, x + DeckStationMenu.BOX_X, y + DeckStationMenu.BOX_Y);

        // Output slot frame
        drawSlotFrame(guiGraphics, x + DeckStationMenu.OUTPUT_X, y + DeckStationMenu.OUTPUT_Y);

        // Player inventory slot frames
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
        // Title in header
        guiGraphics.drawString(font, title, 10, HEADER_Y + 3, TITLE_COLOR, false);

        // Module grid section label
        guiGraphics.drawString(font, "Modules", MODULE_PANEL_X + 4, MODULE_PANEL_Y + 2, TEXT_COLOR, false);

        // Core / Box labels - centered above the slots, sitting in the gap area
        int coreCenter = DeckStationMenu.CORE_X + 8;
        int boxCenter = DeckStationMenu.BOX_X + 8;
        drawCenteredLabel(guiGraphics, "Core", coreCenter, MODULE_PANEL_Y + 2, ACCENT_COLOR);
        drawCenteredLabel(guiGraphics, "Box", boxCenter, MODULE_PANEL_Y + 2, ACCENT_COLOR);

        // Output label - above the output slot
        int outputCenter = DeckStationMenu.OUTPUT_X + 8;
        drawCenteredLabel(guiGraphics, "Output", outputCenter, DeckStationMenu.OUTPUT_Y - 10, ACCENT_COLOR);

        // --- Right panel: Deck Preview ---
        int rpx = RIGHT_PANEL_X + 6;
        int rpy = RIGHT_PANEL_Y + 4;

        guiGraphics.drawString(font, "Deck Preview", rpx, rpy, TITLE_COLOR, false);

        // Thin divider below title
        guiGraphics.fill(RIGHT_PANEL_X + 4, RIGHT_PANEL_Y + 15, RIGHT_PANEL_X + RIGHT_PANEL_W - 4, RIGHT_PANEL_Y + 16,
                UIScreenTheme.Crafting.PANEL_BORDER);

        int infoY = rpy + 16;
        int used = menu.usedPoints();
        int capacity = menu.capacity();
        int modules = menu.moduleCount();
        int maxIntegrity = menu.maxIntegrity();
        boolean overLimit = capacity > 0 && used > capacity;
        int pointsColor = capacity <= 0 ? MUTED_COLOR : (overLimit ? ERROR_COLOR : SUCCESS_COLOR);

        guiGraphics.drawString(font, "Modules: " + modules, rpx, infoY, TEXT_COLOR, false);
        guiGraphics.drawString(font, "Points: " + used + "/" + capacity, rpx, infoY + 12, pointsColor, false);
        guiGraphics.drawString(font, "Max Integrity: " + maxIntegrity, rpx, infoY + 24, TEXT_COLOR, false);

        if (menu.validPreview()) {
            guiGraphics.drawString(font, "Status: Valid", rpx, infoY + 36, SUCCESS_COLOR, false);
        } else {
            String reason = Component.translatable(menu.failureKey().isBlank() ? "incore.cards.deck.missing_core" : menu.failureKey()).getString();
            guiGraphics.drawString(font, "Status: Invalid", rpx, infoY + 36, ERROR_COLOR, false);
            drawWrapped(guiGraphics, reason, rpx, infoY + 48, RIGHT_PANEL_W - 12, 2, UIScreenTheme.Crafting.REASON_TEXT);
        }

        // Modifiers section
        int modY = infoY + 68;
        guiGraphics.drawString(font, "Modifiers", rpx, modY, UIScreenTheme.Crafting.MODIFIER_LABEL_TEXT, false);
        List<String> lines = menu.previewModifierLines();
        if (lines.isEmpty()) {
            guiGraphics.drawString(font, "No active modifiers.", rpx, modY + 12, MUTED_COLOR, false);
        } else {
            for (int i = 0; i < Math.min(5, lines.size()); i++) {
                String line = lines.get(i);
                int color = line.startsWith("-")
                        ? ERROR_COLOR
                        : (line.startsWith("Undecrypted Cryptics:") ? WARNING_COLOR : SUCCESS_COLOR);
                guiGraphics.drawString(font, line, rpx, modY + 12 + i * 10, color, false);
            }
        }

        // Inventory label
        guiGraphics.drawString(font, playerInventoryTitle, INVENTORY_PANEL_X + 4, INVENTORY_PANEL_Y + 4, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        int textWidth = font.width(text);
        guiGraphics.drawString(font, text, centerX - textWidth / 2, y, color, false);
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
