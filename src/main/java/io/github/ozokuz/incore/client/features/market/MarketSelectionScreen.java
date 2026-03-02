package io.github.ozokuz.incore.client.features.market;

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
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MarketSelectionScreen extends Screen implements MarketPayloadUpdatable {
    private static final int PANEL_X = 14;
    private static final int PANEL_Y = 36;
    private static final int PANEL_MARGIN_BOTTOM = 30;
    private static final int TILE_WIDTH = 136;
    private static final int TILE_HEIGHT = 42;
    private static final int TILE_GAP = 6;
    private static final int GRID_PADDING_LEFT = 6;
    private static final int GRID_PADDING_TOP = 6;
    private static final float COST_SCALE = 0.75F;
    private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

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

        int columns = visibleColumns();
        int rowsVisible = visibleRows();
        int startIndex = scrollRow * columns;
        int drawX = gridStartX();
        int drawY = gridStartY();

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
                    ResourceLocation selectedResourceId = MarketScreenDataUtil.parseItemId(item.itemId());
                    if (selectedResourceId != null) {
                        requestItemDetails(selectedResourceId);
                    }
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
        renderBalancePanel(guiGraphics, width - 200, 14, width - 16, 32);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.selection.hint"), 14, 26, 0xCFD6E2, false);

        drawPanel(guiGraphics, PANEL_X - 2, PANEL_Y - 2, panelWidth() + 4, panelHeight() + 4, 0xAA151920, 0xFF454F63);

        List<MarketService.ItemView> ordered = MarketScreenDataUtil.orderedItems(data);
        if (ordered.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_data"), gridStartX(), gridStartY(), 0xDD8D8D, false);
            return;
        }

        int columns = visibleColumns();
        int rowsVisible = visibleRows();
        int startIndex = scrollRow * columns;

        for (int row = 0; row < rowsVisible; row++) {
            for (int col = 0; col < columns; col++) {
                int index = startIndex + row * columns + col;
                if (index >= ordered.size()) {
                    continue;
                }

                MarketService.ItemView item = ordered.get(index);
                int tileX = gridStartX() + col * (TILE_WIDTH + TILE_GAP);
                int tileY = gridStartY() + row * (TILE_HEIGHT + TILE_GAP);
                boolean selected = item.itemId().equals(selectedItemId);

                int borderColor = selected ? 0xFF89C9FF : 0xFF3D4558;
                int fillColor = selected ? 0xFF283446 : 0xFF1B212C;
                drawPanel(guiGraphics, tileX, tileY, TILE_WIDTH, TILE_HEIGHT, fillColor, borderColor);

                renderItemIcon(guiGraphics, item.itemId(), tileX + 4, tileY + 4);
                if (item.inventoryCount() > 0) {
                    String countText = "x" + item.inventoryCount();
                    guiGraphics.drawString(font, countText, tileX + 4, tileY + 20, 0xB7C1D0, false);
                }

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

    private void requestItemDetails(ResourceLocation itemId) {
        Long terminalPos = data == null ? null : data.terminalPos();
        if (terminalPos != null) {
            MarketNetworking.sendRefresh(terminalPos, itemId);
        } else {
            MarketNetworking.requestOpenMarketScreen(itemId);
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
        int columns = visibleColumns();
        int rowsVisible = visibleRows();
        int totalRows = (ordered.size() + columns - 1) / columns;
        return Math.max(0, totalRows - rowsVisible);
    }

    private int gridStartX() {
        return PANEL_X + GRID_PADDING_LEFT;
    }

    private int gridStartY() {
        return PANEL_Y + GRID_PADDING_TOP;
    }

    private int gridWidth() {
        return panelWidth() - GRID_PADDING_LEFT;
    }

    private int gridHeight() {
        return panelHeight() - GRID_PADDING_TOP;
    }

    private int visibleColumns() {
        return Math.max(1, (gridWidth() + TILE_GAP) / (TILE_WIDTH + TILE_GAP));
    }

    private int visibleRows() {
        return Math.max(1, (gridHeight() + TILE_GAP) / (TILE_HEIGHT + TILE_GAP));
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

    private void renderBalancePanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        guiGraphics.fill(left, top, right, bottom, 0xAA141414);

        ItemStack spurIcon = spurIconStack();
        String text = "x" + data.balanceSpur();
        int scaledLineHeight = (int) Math.ceil(16 * COST_SCALE);
        int rowY = top + Math.max(0, (bottom - top - scaledLineHeight) / 2);

        int textWidth = font.width(text);
        int iconWidth = spurIcon.isEmpty() ? 0 : 20;
        int totalWidth = iconWidth + textWidth;
        int scaledWidth = (int) Math.ceil(totalWidth * COST_SCALE);
        int lineX = left + Math.max(0, (right - left - scaledWidth) / 2);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(lineX, rowY, 0.0F);
        guiGraphics.pose().scale(COST_SCALE, COST_SCALE, 1.0F);

        int textX = 0;
        if (!spurIcon.isEmpty()) {
            guiGraphics.renderItem(spurIcon, 0, 0);
            textX = 20;
        }

        guiGraphics.drawString(font, Component.literal(text), textX, 4, 0xBDE8BD, false);
        guiGraphics.pose().popPose();
    }

    private ItemStack spurIconStack() {
        Item item = BuiltInRegistries.ITEM.get(SPUR_ICON_ITEM);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
}
