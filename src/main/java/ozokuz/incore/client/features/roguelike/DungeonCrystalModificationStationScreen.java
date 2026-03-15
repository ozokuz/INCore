package ozokuz.incore.client.features.roguelike;

import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.roguelike.content.DungeonCrystalModificationStationMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;

public class DungeonCrystalModificationStationScreen extends AbstractContainerScreen<DungeonCrystalModificationStationMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.CRAFTING;
    private static final int INPUT_PANEL_X = 8;
    private static final int INPUT_PANEL_Y = 24;
    private static final int INPUT_PANEL_W = 138;
    private static final int INPUT_PANEL_H = 78;
    private static final int RESULT_PANEL_X = 150;
    private static final int RESULT_PANEL_Y = 24;
    private static final int RESULT_PANEL_W = 90;
    private static final int RESULT_PANEL_H = 78;
    private static final int INVENTORY_PANEL_X = 8;
    private static final int INVENTORY_PANEL_Y = 106;
    private static final int INVENTORY_PANEL_W = 232;
    private static final int INVENTORY_PANEL_H = 96;

    private static final int TITLE_TEXT = UIScreenTheme.Crafting.TITLE_TEXT;
    private static final int BODY_TEXT = UIScreenTheme.Crafting.BODY_TEXT;
    private static final int ACCENT_TEXT = UIScreenTheme.Crafting.ACCENT_TEXT;
    private static final int MUTED_TEXT = UIScreenTheme.Crafting.MUTED_TEXT;
    private static final int SUCCESS_TEXT = UIScreenTheme.Crafting.SUCCESS_TEXT;
    private static final int DANGER_TEXT = UIScreenTheme.Crafting.DANGER_TEXT;

    // Section layout
    private static final int HEADER_Y = 5;
    private static final int HEADER_H = 14;
    private static final int SLOTS_PANEL_Y = 22;
    private static final int SLOTS_PANEL_H = 72;
    private static final int STATUS_PANEL_Y = 96;
    private static final int STATUS_PANEL_H = 14;
    private static final int INV_PANEL_Y = 108;

    public DungeonCrystalModificationStationScreen(DungeonCrystalModificationStationMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 248;
        this.imageHeight = 210;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);
        ui.drawWindow(x, y, imageWidth, imageHeight);
        ui.drawPanel(x + 5, y + 5, imageWidth - 10, 14);
        ui.drawPanel(x + INPUT_PANEL_X, y + INPUT_PANEL_Y, INPUT_PANEL_W, INPUT_PANEL_H);
        ui.drawPanel(x + RESULT_PANEL_X, y + RESULT_PANEL_Y, RESULT_PANEL_W, RESULT_PANEL_H);
        ui.drawPanel(x + INVENTORY_PANEL_X, y + INVENTORY_PANEL_Y, INVENTORY_PANEL_W, INVENTORY_PANEL_H);
        drawSlot(guiGraphics, x + DungeonCrystalModificationStationMenu.INPUT_X, y + DungeonCrystalModificationStationMenu.INPUT_Y);
        drawSlot(guiGraphics, x + DungeonCrystalModificationStationMenu.THEME_X, y + DungeonCrystalModificationStationMenu.THEME_Y);
        drawSlot(guiGraphics, x + DungeonCrystalModificationStationMenu.OBJECTIVE_X, y + DungeonCrystalModificationStationMenu.OBJECTIVE_Y);
        for (int modifierX : DungeonCrystalModificationStationMenu.MODIFIER_X) {
            drawSlot(guiGraphics, x + modifierX, y + DungeonCrystalModificationStationMenu.MODIFIER_Y);
        }
        drawSlot(guiGraphics, x + DungeonCrystalModificationStationMenu.OUTPUT_X, y + DungeonCrystalModificationStationMenu.OUTPUT_Y);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlot(guiGraphics,
                        x + DungeonCrystalModificationStationMenu.PLAYER_INV_X + col * 18,
                        y + DungeonCrystalModificationStationMenu.PLAYER_INV_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlot(guiGraphics,
                    x + DungeonCrystalModificationStationMenu.PLAYER_INV_X + col * 18,
                    y + DungeonCrystalModificationStationMenu.HOTBAR_Y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 11, 9, UIScreenTheme.Crafting.TITLE_TEXT, false);
        drawCenteredLabel(guiGraphics, Component.translatable("screen.incore.dungeon_crystal_modification_station.input"), DungeonCrystalModificationStationMenu.INPUT_X + 8, 28);
        drawCenteredLabel(guiGraphics, Component.translatable("screen.incore.dungeon_crystal_modification_station.theme"), DungeonCrystalModificationStationMenu.THEME_X + 8, 28);
        drawCenteredLabel(guiGraphics, Component.translatable("screen.incore.dungeon_crystal_modification_station.objective"), DungeonCrystalModificationStationMenu.OBJECTIVE_X + 8, 28);
        guiGraphics.drawString(font, Component.translatable("screen.incore.dungeon_crystal_modification_station.modifiers"), 12, 62, UIScreenTheme.Crafting.BODY_TEXT, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.dungeon_crystal_modification_station.output"), RESULT_PANEL_X + 4, 28, UIScreenTheme.Crafting.BODY_TEXT, false);
        int statusColor = menu.validPreview() ? UIScreenTheme.Crafting.SUCCESS_TEXT : UIScreenTheme.Crafting.DANGER_TEXT;
        String statusKey = menu.validPreview()
                ? "screen.incore.dungeon_crystal_modification_station.status.ready"
                : "screen.incore.dungeon_crystal_modification_station.status.invalid";
        drawWrapped(guiGraphics, Component.translatable(statusKey), RESULT_PANEL_X + 4, 62, RESULT_PANEL_W - 4, 2, statusColor);
        drawWrapped(
                guiGraphics,
                Component.translatable("screen.incore.dungeon_crystal_modification_station.hint"),
                RESULT_PANEL_X + 4,
                80,
                RESULT_PANEL_W - 4,
                2,
                UIScreenTheme.Crafting.MUTED_TEXT
        );
        guiGraphics.drawString(font, playerInventoryTitle, INVENTORY_PANEL_X + 4, INVENTORY_PANEL_Y + 5, UIScreenTheme.Crafting.BODY_TEXT, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component text, int centerX, int y, int color) {
        int textWidth = font.width(text);
        guiGraphics.drawString(font, text, centerX - textWidth / 2, y, color, false);
    }

    private static void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        themed(guiGraphics).drawSlotFrame(x, y);
    }

    private void drawCenteredLabel(GuiGraphics guiGraphics, Component text, int centerX, int y) {
        guiGraphics.drawCenteredString(font, text, centerX, y, UIScreenTheme.Crafting.BODY_TEXT);
    }

    private void drawWrapped(GuiGraphics guiGraphics, Component text, int x, int y, int width, int maxLines, int color) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(text, width);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            guiGraphics.drawString(font, lines.get(i), x, y + i * font.lineHeight, color);
        }
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }
}
