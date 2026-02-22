package io.github.ozokuz.incore.features.market.client;

import io.github.ozokuz.incore.features.market.MarketService;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MarketSelectionScreen extends Screen implements MarketPayloadUpdatable {
    private static final int PANEL_X = 14;
    private static final int PANEL_Y = 36;
    private static final int PANEL_MARGIN_BOTTOM = 30;
    private static final int TILE_WIDTH = 136;
    private static final int TILE_HEIGHT = 42;
    private static final int TILE_GAP = 6;

    private MarketService.ScreenData data;
    private int scrollRow;
    private @Nullable String selectedItemId;

    public MarketSelectionScreen(String json) {
        this(MarketScreenDataUtil.parse(json), 0, null);
    }

    public MarketSelectionScreen(MarketService.ScreenData data, int scrollRow, @Nullable String selectedItemId) {
        super(Component.translatable("screen.incore.market.title"));
        this.data = data;
        this.scrollRow = Math.max(0, scrollRow);
        this.selectedItemId = selectedItemId;
    }

    @Override
    public void updatePayload(String json) {
        this.data = MarketScreenDataUtil.parse(json);
        List<MarketService.ItemView> ordered = MarketScreenDataUtil.orderedItems(this.data);
        if (ordered.isEmpty()) {
            selectedItemId = null;
            scrollRow = 0;
        } else if (selectedItemId == null || MarketScreenDataUtil.findItem(data, selectedItemId) == null) {
            selectedItemId = ordered.getFirst().itemId();
        }
        clampScroll();
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width - 90, height - 26, 80, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.refresh"), button -> refreshSnapshot())
                .bounds(width - 186, 10, 82, 20)
                .build());

        clampScroll();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = scrollY > 0 ? -1 : 1;
        scrollRow = Math.max(0, Math.min(scrollRow + delta, maxScrollRows()));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        List<MarketService.ItemView> ordered = MarketScreenDataUtil.orderedItems(data);
        if (ordered.isEmpty()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        int columns = Math.max(1, (panelWidth() + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
        int rowsVisible = Math.max(1, (panelHeight() + TILE_GAP) / (TILE_HEIGHT + TILE_GAP));
        int startIndex = scrollRow * columns;
        int drawX = PANEL_X;
        int drawY = PANEL_Y;

        for (int row = 0; row < rowsVisible; row++) {
            for (int col = 0; col < columns; col++) {
                int index = startIndex + row * columns + col;
                if (index >= ordered.size()) {
                    continue;
                }
                int tileX = drawX + col * (TILE_WIDTH + TILE_GAP);
                int tileY = drawY + row * (TILE_HEIGHT + TILE_GAP);
                if (mouseX >= tileX && mouseX < tileX + TILE_WIDTH && mouseY >= tileY && mouseY < tileY + TILE_HEIGHT) {
                    MarketService.ItemView item = ordered.get(index);
                    selectedItemId = item.itemId();
                    minecraft.setScreen(new MarketDetailsScreen(data, item.itemId(), scrollRow));
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, this.title, width / 2, 14, 0xF2F2F2);
        if (data == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_data"), PANEL_X, PANEL_Y, 0xDD8D8D, false);
            return;
        }

        Component mode = data.canTrade()
                ? Component.translatable("screen.incore.market.mode.terminal")
                : Component.translatable("screen.incore.market.mode.read_only");
        guiGraphics.drawString(font, mode, PANEL_X, 16, data.canTrade() ? 0x9AE29A : 0xE2C777, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.selection.hint"), 14, 26, 0xCFD6E2, false);

        drawPanel(guiGraphics, PANEL_X - 2, PANEL_Y - 2, panelWidth() + 4, panelHeight() + 4, 0xAA151920, 0xFF454F63);

        List<MarketService.ItemView> ordered = MarketScreenDataUtil.orderedItems(data);
        if (ordered.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_data"), PANEL_X + 6, PANEL_Y + 6, 0xDD8D8D, false);
            return;
        }

        int columns = Math.max(1, (panelWidth() + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
        int rowsVisible = Math.max(1, (panelHeight() + TILE_GAP) / (TILE_HEIGHT + TILE_GAP));
        int startIndex = scrollRow * columns;

        for (int row = 0; row < rowsVisible; row++) {
            for (int col = 0; col < columns; col++) {
                int index = startIndex + row * columns + col;
                if (index >= ordered.size()) {
                    continue;
                }

                MarketService.ItemView item = ordered.get(index);
                int tileX = PANEL_X + col * (TILE_WIDTH + TILE_GAP);
                int tileY = PANEL_Y + row * (TILE_HEIGHT + TILE_GAP);
                boolean selected = item.itemId().equals(selectedItemId);

                int borderColor = selected ? 0xFF89C9FF : 0xFF3D4558;
                int fillColor = selected ? 0xFF283446 : 0xFF1B212C;
                drawPanel(guiGraphics, tileX, tileY, TILE_WIDTH, TILE_HEIGHT, fillColor, borderColor);

                renderItemIcon(guiGraphics, item.itemId(), tileX + 4, tileY + 4);

                String name = font.plainSubstrByWidth(item.displayName(), TILE_WIDTH - 30);
                guiGraphics.drawString(font, name, tileX + 24, tileY + 4, 0xECF2FF, false);
                guiGraphics.drawString(font, Component.literal(item.currentPriceSpur() + " spur"), tileX + 24, tileY + 16, 0xCFE4FF, false);

                double change = item.dayChangePercent();
                int changeColor = change > 0D ? 0x6EE780 : (change < 0D ? 0xFF8A8A : 0xD0D0D0);
                String arrow = change > 0D ? "▲" : (change < 0D ? "▼" : "");
                String changeText = String.format("%s%.2f%%", arrow.isEmpty() ? "" : arrow + " ", Math.abs(change));
                if (change < 0D) {
                    changeText = String.format("%s %.2f%%", arrow, Math.abs(change));
                }
                guiGraphics.drawString(font, Component.literal(changeText), tileX + 24, tileY + 28, changeColor, false);
            }
        }

        int totalRows = (ordered.size() + columns - 1) / columns;
        int currentRow = Math.min(totalRows, scrollRow + 1);
        guiGraphics.drawString(font, Component.literal(currentRow + "/" + Math.max(1, totalRows)), width - 52, height - 38, 0xB7C1D0, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void refreshSnapshot() {
        Long terminalPos = data == null ? null : data.terminalPos();
        if (terminalPos != null) {
            MarketNetworking.sendRefresh(terminalPos);
        } else {
            MarketNetworking.requestOpenMarketScreen();
        }
    }

    private int panelWidth() {
        return width - PANEL_X * 2;
    }

    private int panelHeight() {
        return height - PANEL_Y - PANEL_MARGIN_BOTTOM;
    }

    private int maxScrollRows() {
        List<MarketService.ItemView> ordered = MarketScreenDataUtil.orderedItems(data);
        int columns = Math.max(1, (panelWidth() + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
        int rowsVisible = Math.max(1, (panelHeight() + TILE_GAP) / (TILE_HEIGHT + TILE_GAP));
        int totalRows = (ordered.size() + columns - 1) / columns;
        return Math.max(0, totalRows - rowsVisible);
    }

    private void clampScroll() {
        scrollRow = Math.max(0, Math.min(scrollRow, maxScrollRows()));
    }

    private void renderItemIcon(GuiGraphics guiGraphics, String itemId, int x, int y) {
        ResourceLocation id = MarketScreenDataUtil.parseItemId(itemId);
        if (id == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            return;
        }
        guiGraphics.renderItem(new ItemStack(item), x, y);
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
}
