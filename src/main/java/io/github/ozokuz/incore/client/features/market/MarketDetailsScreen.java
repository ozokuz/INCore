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

public class MarketDetailsScreen extends Screen implements MarketPayloadUpdatable {
    private static final float COST_SCALE = 0.75F;
    private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

    private MarketService.ScreenData data;
    private String selectedItemId;
    private int selectionScrollRow;
    private @Nullable String requestedHistoryForItemId;

    public MarketDetailsScreen(MarketService.ScreenData data, String selectedItemId, int selectionScrollRow) {
        super(Component.translatable("screen.incore.market.details.title"));
        this.data = data;
        this.selectedItemId = selectedItemId;
        this.selectionScrollRow = Math.max(0, selectionScrollRow);
    }

    @Override
    public void updatePayload(String json) {
        this.data = MarketScreenDataUtil.parse(json);
        List<MarketService.ItemView> ordered = MarketScreenDataUtil.orderedItems(data);
        if (ordered.isEmpty()) {
            selectedItemId = null;
            requestedHistoryForItemId = null;
            return;
        }
        if (selectedItemId == null || MarketScreenDataUtil.findItem(data, selectedItemId) == null) {
            selectedItemId = ordered.getFirst().itemId();
        }
        MarketService.ItemView selected = selectedItem();
        if (selected != null && selected.candles() != null && !selected.candles().isEmpty()) {
            requestedHistoryForItemId = null;
        }
        if (minecraft != null) {
            rebuildWidgets();
        }
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.back"), button -> {
                    minecraft.setScreen(new MarketSelectionScreen(data, selectionScrollRow, selectedItemId));
                }).bounds(16, 14, 60, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.refresh"), button -> {
                    requestSelectedSnapshot();
                }).bounds(width - 98, 14, 82, 20)
                .build());

        if (data == null || !data.canTrade() || data.terminalPos() == null || selectedItem() == null) {
            return;
        }

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.buy"), button -> {
                    ResourceLocation itemId = MarketScreenDataUtil.parseItemId(selectedItemId);
                    if (itemId != null) {
                        minecraft.setScreen(new MarketTradeConfirmScreen(this, data, selectedItemId, true));
                    }
                }).bounds(width / 2 - 172, height - 26, 82, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.sell"), button -> {
                    ResourceLocation itemId = MarketScreenDataUtil.parseItemId(selectedItemId);
                    if (itemId != null) {
                        minecraft.setScreen(new MarketTradeConfirmScreen(this, data, selectedItemId, false));
                    }
                }).bounds(width / 2 - 86, height - 26, 82, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, this.title, width / 2, 16, 0xF2F2F2);
        if (data == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_data"), 16, 44, 0xDD8D8D, false);
            return;
        }

        Component mode = data.canTrade()
                ? Component.translatable("screen.incore.market.mode.terminal")
                : Component.translatable("screen.incore.market.mode.read_only");
        guiGraphics.drawString(font, mode, 84, 20, data.canTrade() ? 0x9AE29A : 0xE2C777, false);
        renderBalancePanel(guiGraphics, 200, 14, width - 16, 32);

        MarketService.ItemView selected = selectedItem();
        if (selected == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_selection"), 16, 44, 0xD0D0D0, false);
            return;
        }
        requestSelectedHistoryIfMissing(selected);

        drawPanel(guiGraphics, 12, 40, width - 24, 76, 0xAA1B212B, 0xFF475063);
        drawPanel(guiGraphics, 12, 122, width - 24, 84, 0xAA1B212B, 0xFF475063);
        drawPanel(guiGraphics, 12, 210, width - 24, Math.max(70, height - 248), 0xAA1B212B, 0xFF475063);

        guiGraphics.drawString(font, Component.literal(selected.displayName()), 42, 48, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.current_price", selected.currentPriceSpur()), 42, 62, 0xCFE4FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.base_price", selected.basePriceSpur()), 42, 74, 0xD0D0D0, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.demand", String.format("%.3f", selected.demandIndex())), 42, 86, 0xD0D0D0, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.owned", selected.inventoryCount()), 42, 98, 0xB7C1D0, false);

        double change = selected.dayChangePercent();
        int changeColor = change > 0D ? 0x6EE780 : (change < 0D ? 0xFF8A8A : 0xD0D0D0);
        String arrow = change > 0D ? "▲" : (change < 0D ? "▼" : "");
        String changeText = arrow.isEmpty()
                ? String.format("%.2f%%", Math.abs(change))
                : String.format("%s %.2f%%", arrow, Math.abs(change));
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.day_change", changeText), 210, 62, changeColor, false);

        renderItemIcon(guiGraphics, selected.itemId(), 18, 52);

        renderChart(
                guiGraphics,
                16,
                126,
                width - 32,
                76,
                Component.translatable("screen.incore.market.chart.intraday"),
                selected.candles(),
                24
        );
        renderChart(
                guiGraphics,
                16,
                214,
                width - 32,
                Math.max(64, height - 254),
                Component.translatable("screen.incore.market.chart.month"),
                selected.candles(),
                24 * 30
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(new MarketSelectionScreen(data, selectionScrollRow, selectedItemId));
            return;
        }
        super.onClose();
    }

    private @Nullable MarketService.ItemView selectedItem() {
        return MarketScreenDataUtil.findItem(data, selectedItemId);
    }

    private void requestSelectedSnapshot() {
        if (selectedItemId == null) {
            Long terminalPos = data == null ? null : data.terminalPos();
            if (terminalPos != null) {
                MarketNetworking.sendRefresh(terminalPos);
            } else {
                MarketNetworking.requestOpenMarketScreen();
            }
            return;
        }

        ResourceLocation itemId = MarketScreenDataUtil.parseItemId(selectedItemId);
        if (itemId == null) {
            return;
        }
        Long terminalPos = data == null ? null : data.terminalPos();
        if (terminalPos != null) {
            MarketNetworking.sendRefresh(terminalPos, itemId);
        } else {
            MarketNetworking.requestOpenMarketScreen(itemId);
        }
        requestedHistoryForItemId = selectedItemId;
    }

    private void requestSelectedHistoryIfMissing(MarketService.ItemView selected) {
        if (selected.candles() != null && !selected.candles().isEmpty()) {
            if (selected.itemId().equals(requestedHistoryForItemId)) {
                requestedHistoryForItemId = null;
            }
            return;
        }
        if (selected.itemId().equals(requestedHistoryForItemId)) {
            return;
        }
        requestSelectedSnapshot();
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

    private void renderChart(
            GuiGraphics guiGraphics,
            int x,
            int y,
            int width,
            int height,
            Component title,
            List<MarketService.CandleView> candles,
            int sampleCount
    ) {
        if (width < 24 || height < 24) {
            return;
        }

        drawPanel(guiGraphics, x, y, width, height, 0x661A1F27, 0xFF424D5F);
        guiGraphics.drawString(font, title, x + 4, y + 4, 0xE8E8E8, false);
        if (candles == null || candles.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.chart.empty"), x + 4, y + 16, 0xC6C6C6, false);
            return;
        }

        int from = Math.max(0, candles.size() - Math.max(1, sampleCount));
        List<MarketService.CandleView> sample = candles.subList(from, candles.size());
        if (sample.size() < 2) {
            return;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (MarketService.CandleView candle : sample) {
            min = Math.min(min, candle.close());
            max = Math.max(max, candle.close());
        }
        if (min == Integer.MAX_VALUE || max == Integer.MIN_VALUE) {
            return;
        }
        if (min == max) {
            max = min + 1;
        }

        int graphX = x + 4;
        int graphY = y + 18;
        int graphWidth = width - 8;
        int graphHeight = height - 22;
        int prevX = -1;
        int prevY = -1;
        for (int i = 0; i < sample.size(); i++) {
            MarketService.CandleView candle = sample.get(i);
            int px = graphX + (i * (graphWidth - 1)) / (sample.size() - 1);
            int py = graphY + graphHeight - 1 - ((candle.close() - min) * (graphHeight - 1)) / (max - min);

            if (prevX >= 0) {
                drawLine(guiGraphics, prevX, prevY, px, py, 0xFF71C2FF);
            }
            prevX = px;
            prevY = py;
        }

        guiGraphics.drawString(font, Component.literal(Integer.toString(max)), x + width - 30, y + 4, 0xD5D5D5, false);
        guiGraphics.drawString(font, Component.literal(Integer.toString(min)), x + width - 30, y + height - 11, 0xBFBFBF, false);
    }

    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps <= 0) {
            guiGraphics.fill(x1, y1, x1 + 1, y1 + 1, color);
            return;
        }

        for (int i = 0; i <= steps; i++) {
            int x = x1 + (dx * i) / steps;
            int y = y1 + (dy * i) / steps;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
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
