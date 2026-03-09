package io.github.ozokuz.incore.client.features.market;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
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
    private static final UIScreenTheme THEME = UIScreenTheme.MARKET_SHOP;
    private static final int PANEL_X = 14;
    private static final int PANEL_Y = 36;
    private static final int PANEL_MARGIN_BOTTOM = 30;
    private static final int TILE_WIDTH = 136;
    private static final int TILE_HEIGHT = 42;
    private static final int TILE_GAP = 6;
    private static final int GRID_PADDING_LEFT = 6;
    private static final int GRID_PADDING_TOP = 6;
    private static final int GRID_PADDING_RIGHT = 14;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_MIN_THUMB_HEIGHT = 16;
    private static final float COST_SCALE = 0.75F;
    private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

    private MarketService.ScreenData data;
    private int scrollRow;
    private @Nullable String selectedItemId;
    private boolean draggingScrollbar;
    private double scrollbarDragOffsetY;

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

        clampScroll();
        MarketNetworking.subscribeMarketView(true, data == null ? null : data.terminalPos(), null);
    }

    @Override
    public void removed() {
        MarketNetworking.subscribeMarketView(false, null, null);
        super.removed();
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
        int itemCount = ordered.size();
        int maxRows = maxScrollRows(itemCount);
        if (maxRows > 0 && isMouseOverScrollbar(mouseX, mouseY)) {
            int thumbY = scrollbarThumbY(itemCount, maxRows);
            int thumbHeight = scrollbarThumbHeight(itemCount);
            if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
                draggingScrollbar = true;
                scrollbarDragOffsetY = mouseY - thumbY;
            } else {
                draggingScrollbar = false;
                setScrollFromThumbTop(mouseY - (thumbHeight / 2.0D), maxRows, thumbHeight);
            }
            return true;
        }

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
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0 || !draggingScrollbar) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        int itemCount = MarketScreenDataUtil.orderedItems(data).size();
        int maxRows = maxScrollRows(itemCount);
        if (maxRows <= 0) {
            draggingScrollbar = false;
            return true;
        }

        int thumbHeight = scrollbarThumbHeight(itemCount);
        setScrollFromThumbTop(mouseY - scrollbarDragOffsetY, maxRows, thumbHeight);
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, this.title, width / 2, 14, UIScreenTheme.MarketShop.TITLE_TEXT);
        if (data == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_data"), PANEL_X, PANEL_Y, UIScreenTheme.MarketShop.NO_DATA_TEXT, false);
            return;
        }

        Component mode = data.canTrade()
                ? Component.translatable("screen.incore.market.mode.terminal")
                : Component.translatable("screen.incore.market.mode.read_only");
        guiGraphics.drawString(font, mode, PANEL_X, 16, data.canTrade() ? UIScreenTheme.MarketShop.MODE_ACTIVE_TEXT : UIScreenTheme.MarketShop.MODE_WARNING_TEXT, false);
        renderBalancePanel(guiGraphics, width - 200, 14, width - 16, 32);

        drawPanel(guiGraphics, PANEL_X - 2, PANEL_Y - 2, panelWidth() + 4, panelHeight() + 4, UIScreenTheme.MarketShop.OUTER_PANEL_FILL, UIScreenTheme.MarketShop.OUTER_PANEL_BORDER);

        List<MarketService.ItemView> ordered = MarketScreenDataUtil.orderedItems(data);
        if (ordered.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_data"), gridStartX(), gridStartY(), UIScreenTheme.MarketShop.NO_DATA_TEXT, false);
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

                int borderColor = selected ? UIScreenTheme.MarketShop.ITEM_TILE_BORDER_SELECTED : UIScreenTheme.MarketShop.ITEM_TILE_BORDER;
                int fillColor = selected ? UIScreenTheme.MarketShop.ITEM_TILE_FILL_SELECTED : UIScreenTheme.MarketShop.ITEM_TILE_FILL;
                drawPanel(guiGraphics, tileX, tileY, TILE_WIDTH, TILE_HEIGHT, fillColor, borderColor);

                renderItemIcon(guiGraphics, item.itemId(), tileX + 4, tileY + 4);
                if (item.inventoryCount() > 0) {
                    String countText = "x" + item.inventoryCount();
                    guiGraphics.drawString(font, countText, tileX + 4, tileY + 20, UIScreenTheme.MarketShop.TEXT_MUTED, false);
                }

                String name = font.plainSubstrByWidth(item.displayName(), TILE_WIDTH - 30);
                guiGraphics.drawString(font, name, tileX + 24, tileY + 4, UIScreenTheme.MarketShop.TEXT_PRIMARY, false);
                guiGraphics.drawString(font, Component.literal(item.currentPriceSpur() + " spur"), tileX + 24, tileY + 16, UIScreenTheme.MarketShop.TEXT_ACCENT, false);

                double change = item.dayChangePercent();
                int changeColor = change > 0D ? UIScreenTheme.MarketShop.TEXT_POSITIVE : (change < 0D ? UIScreenTheme.MarketShop.TEXT_NEGATIVE : UIScreenTheme.MarketShop.TEXT_NEUTRAL);
                String arrow = change > 0D ? "▲" : (change < 0D ? "▼" : "");
                String changeText = String.format("%s%.2f%%", arrow.isEmpty() ? "" : arrow + " ", Math.abs(change));
                if (change < 0D) {
                    changeText = String.format("%s %.2f%%", arrow, Math.abs(change));
                }
                guiGraphics.drawString(font, Component.literal(changeText), tileX + 24, tileY + 28, changeColor, false);
            }
        }

        renderScrollbar(guiGraphics, ordered.size());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
        return maxScrollRows(MarketScreenDataUtil.orderedItems(data).size());
    }

    private int maxScrollRows(int itemCount) {
        int columns = visibleColumns();
        int rowsVisible = visibleRows();
        int totalRows = (itemCount + columns - 1) / columns;
        return Math.max(0, totalRows - rowsVisible);
    }

    private int gridStartX() {
        return PANEL_X + GRID_PADDING_LEFT;
    }

    private int gridStartY() {
        return PANEL_Y + GRID_PADDING_TOP;
    }

    private int gridWidth() {
        return Math.max(1, panelWidth() - GRID_PADDING_LEFT - GRID_PADDING_RIGHT);
    }

    private int gridHeight() {
        return Math.max(1, panelHeight() - GRID_PADDING_TOP);
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

    private int scrollbarX() {
        return PANEL_X + panelWidth() - GRID_PADDING_RIGHT + Math.max(0, (GRID_PADDING_RIGHT - SCROLLBAR_WIDTH) / 2);
    }

    private int scrollbarTrackTop() {
        return gridStartY();
    }

    private int scrollbarTrackHeight() {
        return gridHeight();
    }

    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int x = scrollbarX();
        int y = scrollbarTrackTop();
        return mouseX >= x && mouseX < x + SCROLLBAR_WIDTH && mouseY >= y && mouseY < y + scrollbarTrackHeight();
    }

    private int scrollbarThumbHeight(int itemCount) {
        int rowsVisible = visibleRows();
        int totalRows = (itemCount + visibleColumns() - 1) / visibleColumns();
        if (totalRows <= 0) {
            return scrollbarTrackHeight();
        }
        int rawHeight = (int) Math.round((double) rowsVisible / (double) totalRows * scrollbarTrackHeight());
        return Math.min(scrollbarTrackHeight(), Math.max(SCROLLBAR_MIN_THUMB_HEIGHT, rawHeight));
    }

    private int scrollbarThumbY(int itemCount, int maxRows) {
        int trackTop = scrollbarTrackTop();
        if (maxRows <= 0) {
            return trackTop;
        }
        int thumbHeight = scrollbarThumbHeight(itemCount);
        int range = Math.max(1, scrollbarTrackHeight() - thumbHeight);
        return trackTop + (int) Math.round((double) scrollRow / (double) maxRows * range);
    }

    private void setScrollFromThumbTop(double thumbTop, int maxRows, int thumbHeight) {
        if (maxRows <= 0) {
            scrollRow = 0;
            return;
        }
        int trackTop = scrollbarTrackTop();
        int range = Math.max(1, scrollbarTrackHeight() - thumbHeight);
        double clampedTop = Math.max(trackTop, Math.min(thumbTop, trackTop + range));
        double progress = (clampedTop - trackTop) / range;
        scrollRow = (int) Math.round(progress * maxRows);
        clampScroll();
    }

    private void renderScrollbar(GuiGraphics guiGraphics, int itemCount) {
        int x = scrollbarX();
        int y = scrollbarTrackTop();
        int trackHeight = scrollbarTrackHeight();
        drawPanel(guiGraphics, x, y, SCROLLBAR_WIDTH, trackHeight, UIScreenTheme.MarketShop.SCROLLBAR_TRACK_FILL, UIScreenTheme.MarketShop.ITEM_TILE_BORDER);

        int maxRows = maxScrollRows(itemCount);
        if (maxRows <= 0) {
            return;
        }

        int thumbHeight = scrollbarThumbHeight(itemCount);
        int thumbY = scrollbarThumbY(itemCount, maxRows);
        int thumbColor = draggingScrollbar ? UIScreenTheme.MarketShop.SCROLLBAR_THUMB_FILL_ACTIVE : UIScreenTheme.MarketShop.SCROLLBAR_THUMB_FILL;
        drawPanel(guiGraphics, x + 1, thumbY, Math.max(1, SCROLLBAR_WIDTH - 2), thumbHeight, thumbColor, UIScreenTheme.MarketShop.SCROLLBAR_THUMB_BORDER);
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
        guiGraphics.fill(left, top, right, bottom, UIScreenTheme.MarketShop.OVERLAY_PANEL_FILL);

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

        guiGraphics.drawString(font, Component.literal(text), textX, 4, UIScreenTheme.MarketShop.OVERLAY_VALUE_TEXT, false);
        guiGraphics.pose().popPose();
    }

    private ItemStack spurIconStack() {
        Item item = BuiltInRegistries.ITEM.get(SPUR_ICON_ITEM);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        ThemedUi ui = themed(guiGraphics);
        ui.drawRect(x, y, x + width, y + height, fillColor);
        ui.drawBorder(x, y, x + width, y + height, borderColor);
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }
}
