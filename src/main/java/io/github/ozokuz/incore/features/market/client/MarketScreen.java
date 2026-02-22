package io.github.ozokuz.incore.features.market.client;

import com.google.gson.Gson;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class MarketScreen extends Screen {
    private static final Gson GSON = new Gson();
    private static final int ITEMS_PER_PAGE = 10;

    private MarketService.ScreenData data;
    private String selectedItemId;
    private int page;
    private int quantity = 1;

    public MarketScreen(String json) {
        super(Component.translatable("screen.incore.market.title"));
        this.data = parse(json);
    }

    public void updatePayload(String json) {
        this.data = parse(json);
        if (minecraft != null) {
            if (selectedItemId == null || orderedItems().stream().noneMatch(item -> selectedItemId.equals(item.itemId()))) {
                selectedItemId = orderedItems().isEmpty() ? null : orderedItems().getFirst().itemId();
            }
            page = Math.clamp(page, 0, Math.max(0, ((orderedItems().size() - 1) / ITEMS_PER_PAGE)));
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

        List<MarketService.ItemView> ordered = orderedItems();
        if (selectedItemId == null || ordered.stream().noneMatch(item -> selectedItemId.equals(item.itemId()))) {
            selectedItemId = ordered.isEmpty() ? null : ordered.getFirst().itemId();
        }
        page = Math.clamp(page, 0, Math.max(0, ((ordered.size() - 1) / ITEMS_PER_PAGE)));

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width - 90, height - 26, 80, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildWidgets();
                }).bounds(16, 14, 24, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                    int maxPage = Math.max(0, ((ordered.size() - 1) / ITEMS_PER_PAGE));
                    page = Math.min(maxPage, page + 1);
                    rebuildWidgets();
                }).bounds(44, 14, 24, 20)
                .build());

        Long terminalPos = data == null ? null : data.terminalPos();
        boolean canTrade = data != null && data.canTrade() && terminalPos != null;

        if (terminalPos != null) {
            addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.refresh"), button -> {
                        MarketNetworking.sendRefresh(terminalPos);
                    }).bounds(width - 186, 14, 82, 20)
                    .build());
        } else {
            addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.refresh"), button -> {
                        MarketNetworking.requestOpenMarketScreen();
                    }).bounds(width - 186, 14, 82, 20)
                    .build());
        }

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(ordered.size(), start + ITEMS_PER_PAGE);
        int y = 38;
        for (int i = start; i < end; i++) {
            MarketService.ItemView item = ordered.get(i);
            String selected = item.itemId().equals(selectedItemId) ? "[x] " : "";
            String label = selected + item.displayName() + " [" + item.currentPriceSpur() + "]";
            addRenderableWidget(Button.builder(Component.literal(label), button -> {
                        selectedItemId = item.itemId();
                        rebuildWidgets();
                    }).bounds(16, y, 206, 18)
                    .build());
            y += 20;
        }

        if (!canTrade || selectedItemId == null) {
            return;
        }

        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                    quantity = Math.max(1, quantity - 1);
                    rebuildWidgets();
                }).bounds(240, height - 26, 20, 20)
                .build());
        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    quantity = Math.min(64, quantity + 1);
                    rebuildWidgets();
                }).bounds(364, height - 26, 20, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.buy"), button -> {
                    ResourceLocation itemId = ResourceLocation.tryParse(selectedItemId);
                    if (itemId != null) {
                        MarketNetworking.sendBuy(terminalPos, itemId, quantity);
                    }
                }).bounds(388, height - 26, 82, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.sell"), button -> {
                    ResourceLocation itemId = ResourceLocation.tryParse(selectedItemId);
                    if (itemId != null) {
                        MarketNetworking.sendSell(terminalPos, itemId, quantity);
                    }
                }).bounds(474, height - 26, 82, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.trust_add"), button -> {
                    MarketNetworking.sendTrustAdd(terminalPos);
                }).bounds(width - 186, 38, 82, 20)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.incore.market.trust_remove"), button -> {
                    MarketNetworking.sendTrustRemove(terminalPos);
                }).bounds(width - 100, 38, 84, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, title, width / 2, 8, 0xF2F2F2);

        if (data == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_data"), 16, 38, 0xDD8D8D, false);
            return;
        }

        int totalPages = Math.max(1, (orderedItems().size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.page", page + 1, totalPages), 74, 20, 0xD0D0D0, false);

        Component mode = data.canTrade()
                ? Component.translatable("screen.incore.market.mode.terminal")
                : Component.translatable("screen.incore.market.mode.read_only");
        guiGraphics.drawString(font, mode, 240, 20, data.canTrade() ? 0x9AE29A : 0xE2C777, false);

        if (selectedItemId == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.market.no_selection"), 240, 54, 0xD0D0D0, false);
            return;
        }

        MarketService.ItemView selected = selectedItem();
        if (selected == null) {
            return;
        }

        guiGraphics.drawString(font, Component.literal(selected.displayName()), 268, 42, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.current_price", selected.currentPriceSpur()), 268, 56, 0xCFE4FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.base_price", selected.basePriceSpur()), 268, 68, 0xD0D0D0, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.demand", String.format("%.3f", selected.demandIndex())), 268, 80, 0xD0D0D0, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.market.trade_quantity", quantity), 266, height - 20, 0xD0D0D0, false);

        ResourceLocation itemId = ResourceLocation.tryParse(selected.itemId());
        if (itemId != null) {
            Item item = BuiltInRegistries.ITEM.get(itemId);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                guiGraphics.renderItem(new ItemStack(item), 240, 40);
            }
        }

        renderChart(
                guiGraphics,
                240,
                96,
                Math.min(332, width - 256),
                76,
                Component.translatable("screen.incore.market.chart.intraday"),
                selected.candles(),
                24
        );
        renderChart(
                guiGraphics,
                240,
                178,
                Math.min(332, width - 256),
                94,
                Component.translatable("screen.incore.market.chart.month"),
                selected.candles(),
                24 * 30
        );
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private MarketService.ItemView selectedItem() {
        for (MarketService.ItemView item : orderedItems()) {
            if (item.itemId().equals(selectedItemId)) {
                return item;
            }
        }
        return null;
    }

    private List<MarketService.ItemView> orderedItems() {
        if (data == null || data.items() == null || data.items().isEmpty()) {
            return List.of();
        }

        List<MarketService.ItemView> copy = new ArrayList<>(data.items());
        copy.sort(Comparator.comparing(MarketService.ItemView::displayName, String.CASE_INSENSITIVE_ORDER));
        return copy;
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

        guiGraphics.fill(x, y, x + width, y + height, 0x88202020);
        guiGraphics.fill(x, y, x + width, y + 1, 0xFF4A4A4A);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF4A4A4A);
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF4A4A4A);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF4A4A4A);

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

        guiGraphics.drawString(font, Component.literal(Integer.toString(max)), x + width - 28, y + 4, 0xD5D5D5, false);
        guiGraphics.drawString(font, Component.literal(Integer.toString(min)), x + width - 28, y + height - 11, 0xBFBFBF, false);
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

    private static MarketService.ScreenData parse(String json) {
        MarketService.ScreenData parsed = GSON.fromJson(json, MarketService.ScreenData.class);
        if (parsed == null || parsed.items() == null) {
            return new MarketService.ScreenData(false, null, List.of(), List.of());
        }
        List<String> trusted = parsed.trustedPlayers() == null ? List.of() : parsed.trustedPlayers();
        return new MarketService.ScreenData(parsed.canTrade(), parsed.terminalPos(), parsed.items(), trusted);
    }
}
