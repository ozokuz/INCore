package io.github.ozokuz.incore.features.cards.client;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.cards.CardItemFactory;
import io.github.ozokuz.incore.features.cards.CardVendorService;
import io.github.ozokuz.incore.features.cards.network.CardNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CardVendorScreen extends Screen {
    private static final int ROWS_PER_PAGE = 5;
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_TOP = 16;
    private static final int PANEL_BOTTOM_MARGIN = 36;
    private static final int ROW_HEIGHT = 48;
    private static final float COST_SCALE = 0.75F;
    private static final ResourceLocation SPUR_ID = ResourceLocation.parse("numismatics:spur");

    private final CardVendorService.VendorScreenData data;
    private final Map<String, Integer> quantities = new HashMap<>();
    private int page;
    private Integer previousMenuBlur;

    public CardVendorScreen(CardVendorService.VendorScreenData data) {
        super(Component.translatable("screen.incore.cards.vendor.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        if (this.previousMenuBlur == null && this.minecraft != null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }
        rebuildVendorWidgets();
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.previousMenuBlur != null) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.previousMenuBlur);
        }
        this.previousMenuBlur = null;
        super.removed();
    }

    private void rebuildVendorWidgets() {
        clearWidgets();
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = this.width / 2 + PANEL_WIDTH / 2;
        int rowsTop = PANEL_TOP + 34;
        List<CardVendorService.VendorOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        page = Math.clamp(page, 0, maxPages - 1);
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            CardVendorService.VendorOfferView offer = offers.get(i);
            int row = i - start;
            int y = rowsTop + row * ROW_HEIGHT;
            int quantity = selectedQuantity(offer);

            Button minusButton = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                        adjustQuantity(offer, -1);
                    })
                    .bounds(right - 132, y + 13, 16, 20)
                    .build());
            minusButton.active = quantity > 1;

            Button plusButton = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                        adjustQuantity(offer, 1);
                    })
                    .bounds(right - 94, y + 13, 16, 20)
                    .build());
            plusButton.active = quantity > 0 && quantity < maxSelectableQuantity(offer);

            Button buyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.cards.vendor.buy"), button -> {
                        onBuyOffer(offer);
                    })
                    .bounds(right - 74, y + 13, 64, 20)
                    .build());
            buyButton.active = isPurchasable(offer, quantity);
        }

        Button prevButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.cards.vendor.prev"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildVendorWidgets();
                })
                .bounds(left + 8, this.height - 52, 58, 18)
                .build());
        prevButton.active = page > 0;

        Button nextButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.cards.vendor.next"), button -> {
                    page = Math.min(maxPages - 1, page + 1);
                    rebuildVendorWidgets();
                })
                .bounds(left + 70, this.height - 52, 58, 18)
                .build());
        nextButton.active = page < maxPages - 1;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = this.width / 2 + PANEL_WIDTH / 2;
        int top = PANEL_TOP;
        int bottom = this.height - PANEL_BOTTOM_MARGIN;

        guiGraphics.fill(left, top, right, bottom, 0xCC120E18);
        guiGraphics.fill(left, top, right, top + 1, 0xFF6CE0FF);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF6CE0FF);

        List<CardVendorService.VendorOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        int y = top + 34;
        for (int i = start; i < end; i++) {
            CardVendorService.VendorOfferView offer = offers.get(i);
            int quantity = selectedQuantity(offer);
            boolean soldOut = offer.stockRemaining() <= 0;
            boolean purchasable = isPurchasable(offer, quantity);
            int panelColor = soldOut ? 0x663A1D25 : (purchasable ? 0x66233648 : 0x662C2832);
            int headerColor = soldOut ? 0xFFCE6D6D : (purchasable ? 0xFF66D9FF : 0xFF9FA7B5);
            guiGraphics.fill(left + 6, y, right - 6, y + 44, panelColor);
            guiGraphics.fill(left + 6, y, right - 6, y + 1, headerColor);
            y += ROW_HEIGHT;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 6, 0xECF7FF);
        renderBalancePanel(guiGraphics, left + 8, top + 16, left + 122, top + 30);
        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.cards.vendor.page", page + 1, maxPages),
                right - 56,
                top + 18,
                0xA9E8FF,
                false
        );

        y = top + 34;
        for (int i = start; i < end; i++) {
            CardVendorService.VendorOfferView offer = offers.get(i);
            int quantity = selectedQuantity(offer);
            ItemStack productStack = productStackForOffer(offer);
            if (!productStack.isEmpty()) {
                guiGraphics.renderItem(productStack, left + 10, y + 12);
            }

            String type = offer.productType().toLowerCase(Locale.ROOT);
            guiGraphics.drawString(this.font, offer.name(), left + 32, y + 6, 0xF0F0F0, false);
            guiGraphics.drawString(
                    this.font,
                    Component.literal("x" + offer.count() + "  (" + type + ")"),
                    left + 32,
                    y + 18,
                    0xA2BFD8,
                    false
            );

            if (offer.stockRemaining() > 0) {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.incore.cards.vendor.stock", offer.stockRemaining()),
                        left + 32,
                        y + 30,
                        0xBDE8BD,
                        false
                );
            } else {
                guiGraphics.drawString(
                        this.font,
                        Component.translatable("screen.incore.cards.vendor.sold_out"),
                        left + 32,
                        y + 30,
                        0xFF7777,
                        false
                );
            }

            renderOfferCostPanel(guiGraphics, right - 252, y + 14, right - 138, y + 34, offer, quantity);

            int quantityX = right - 112;
            guiGraphics.drawCenteredString(this.font, Component.literal("x" + quantity), quantityX, y + 19, 0xDDE7F2);

            y += ROW_HEIGHT;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void onBuyOffer(CardVendorService.VendorOfferView offer) {
        int quantity = selectedQuantity(offer);
        if (quantity <= 0) {
            return;
        }

        ResourceLocation offerId = ResourceLocation.tryParse(offer.id());
        if (offerId == null) {
            return;
        }

        if (canAffordByTokens(offer, quantity)) {
            CardNetworking.sendVendorPurchase(offerId, data.vendorPosLong(), quantity, false);
            return;
        }

        if (!canAffordByConversion(offer, quantity)) {
            return;
        }

        int missingTokens = tokenShortfall(offer, quantity);
        int requiredSpur = requiredSpur(offer, quantity);
        Minecraft.getInstance().setScreen(new CardVendorSpurConversionConfirmScreen(
                this,
                offerId,
                offer.name(),
                data.vendorPosLong(),
                quantity,
                missingTokens,
                requiredSpur,
                data.spurCount()
        ));
    }

    private void adjustQuantity(CardVendorService.VendorOfferView offer, int delta) {
        int max = maxSelectableQuantity(offer);
        if (max <= 0) {
            return;
        }

        int current = selectedQuantity(offer);
        int next = Math.clamp(current + delta, 1, max);
        quantities.put(offer.id(), next);
        rebuildVendorWidgets();
    }

    private int selectedQuantity(CardVendorService.VendorOfferView offer) {
        int max = maxSelectableQuantity(offer);
        if (max <= 0) {
            quantities.put(offer.id(), 0);
            return 0;
        }

        int quantity = quantities.getOrDefault(offer.id(), 1);
        quantity = Math.clamp(quantity, 1, max);
        quantities.put(offer.id(), quantity);
        return quantity;
    }

    private int maxSelectableQuantity(CardVendorService.VendorOfferView offer) {
        return Math.max(0, offer.stockRemaining());
    }

    private boolean isPurchasable(CardVendorService.VendorOfferView offer, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        return canAffordByTokens(offer, quantity) || canAffordByConversion(offer, quantity);
    }

    private boolean canAffordByTokens(CardVendorService.VendorOfferView offer, int quantity) {
        return tokenShortfall(offer, quantity) <= 0;
    }

    private boolean canAffordByConversion(CardVendorService.VendorOfferView offer, int quantity) {
        int shortfall = tokenShortfall(offer, quantity);
        long required = (long) shortfall * CardVendorService.SPUR_PER_TOKEN;
        return shortfall > 0 && required <= data.spurCount();
    }

    private int tokenShortfall(CardVendorService.VendorOfferView offer, int quantity) {
        long tokenCost = (long) offer.tokenCost() * quantity;
        long missing = Math.max(0L, tokenCost - data.tokenCount());
        if (missing > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) missing;
    }

    private int requiredSpur(CardVendorService.VendorOfferView offer, int quantity) {
        long required = (long) tokenShortfall(offer, quantity) * CardVendorService.SPUR_PER_TOKEN;
        if (required > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) required;
    }

    private ItemStack productStackForOffer(CardVendorService.VendorOfferView offer) {
        ResourceLocation productId = ResourceLocation.tryParse(offer.productId());
        if (productId == null) {
            return ItemStack.EMPTY;
        }

        return switch (offer.productType().toLowerCase(Locale.ROOT)) {
            case "booster_box" -> CardItemFactory.boosterBox(productId, 1);
            default -> CardItemFactory.booster(productId, 1);
        };
    }

    private void renderOfferCostPanel(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int right,
            int bottom,
            CardVendorService.VendorOfferView offer,
            int quantity
    ) {
        List<CostRenderLine> lines = buildCostLines(offer, quantity);
        if (lines.isEmpty()) {
            return;
        }

        guiGraphics.fill(left, top, right, bottom, 0xAA141414);

        int scaledLineHeight = (int) Math.ceil(16 * COST_SCALE);
        int rowY = top + Math.max(0, (bottom - top - scaledLineHeight) / 2);
        int totalWidth = 0;
        for (CostRenderLine line : lines) {
            totalWidth += scaledCostLineWidth(line);
        }
        int gap = 4;
        if (lines.size() > 1) {
            totalWidth += gap * (lines.size() - 1);
        }
        int lineX = left + Math.max(0, (right - left - totalWidth) / 2);
        for (CostRenderLine line : lines) {
            renderCostLine(guiGraphics, lineX, rowY, line);
            lineX += scaledCostLineWidth(line) + gap;
        }
    }

    private List<CostRenderLine> buildCostLines(CardVendorService.VendorOfferView offer, int quantity) {
        List<CostRenderLine> lines = new ArrayList<>();
        if (quantity <= 0) {
            return lines;
        }

        long totalTokenCost = (long) offer.tokenCost() * quantity;
        int tokenAvailable = data.tokenCount();
        int tokenCovered = (int) Math.min(totalTokenCost, tokenAvailable);
        long missingTokensRaw = Math.max(0L, totalTokenCost - tokenCovered);
        int missingTokens = missingTokensRaw > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) missingTokensRaw;
        boolean conversionAffordable = missingTokens > 0 && canAffordByConversion(offer, quantity);

        ItemStack tokenIcon = new ItemStack(Registration.CARD_TOKEN_ITEM.get());
        if (tokenCovered > 0) {
            lines.add(new CostRenderLine(tokenIcon, "x" + tokenCovered, 0xBDE8BD));
        }
        if (missingTokens > 0) {
            if (!conversionAffordable) {
                lines.add(new CostRenderLine(tokenIcon, "x" + missingTokens, 0xFF5555));
            }
            int requiredSpur = requiredSpur(offer, quantity);
            lines.add(new CostRenderLine(spurCostIcon(), "x" + requiredSpur, conversionAffordable ? 0xBDE8BD : 0xFF5555));
        }
        if (totalTokenCost <= 0L) {
            lines.add(new CostRenderLine(ItemStack.EMPTY, Component.translatable("screen.incore.cards.vendor.free").getString(), 0xBDE8BD));
        }
        return lines;
    }

    private void renderBalancePanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        List<CostRenderLine> lines = buildBalanceLines();
        if (lines.isEmpty()) {
            return;
        }

        guiGraphics.fill(left, top, right, bottom, 0xAA141414);

        int scaledLineHeight = (int) Math.ceil(16 * COST_SCALE);
        int rowY = top + Math.max(0, (bottom - top - scaledLineHeight) / 2);
        int totalWidth = 0;
        for (CostRenderLine line : lines) {
            totalWidth += scaledCostLineWidth(line);
        }
        int gap = 4;
        if (lines.size() > 1) {
            totalWidth += gap * (lines.size() - 1);
        }
        int lineX = left + Math.max(0, (right - left - totalWidth) / 2);
        for (CostRenderLine line : lines) {
            renderCostLine(guiGraphics, lineX, rowY, line);
            lineX += scaledCostLineWidth(line) + gap;
        }
    }

    private List<CostRenderLine> buildBalanceLines() {
        List<CostRenderLine> lines = new ArrayList<>();
        lines.add(new CostRenderLine(new ItemStack(Registration.CARD_TOKEN_ITEM.get()), "x" + data.tokenCount(), 0xBDE8BD));
        lines.add(new CostRenderLine(spurCostIcon(), "x" + data.spurCount(), 0xBDE8BD));
        return lines;
    }

    private ItemStack spurCostIcon() {
        Item spurItem = BuiltInRegistries.ITEM.get(SPUR_ID);
        return spurItem == Items.AIR ? ItemStack.EMPTY : spurItem.getDefaultInstance();
    }

    private void renderCostLine(GuiGraphics guiGraphics, int x, int y, CostRenderLine line) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(COST_SCALE, COST_SCALE, 1.0F);

        int textX = 0;
        if (!line.stack().isEmpty()) {
            guiGraphics.renderItem(line.stack(), 0, 0);
            textX = 20;
        }

        guiGraphics.drawString(this.font, Component.literal(line.text()), textX, 4, line.color(), false);
        guiGraphics.pose().popPose();
    }

    private int scaledCostLineWidth(CostRenderLine line) {
        int width = this.font.width(line.text());
        if (!line.stack().isEmpty()) {
            width += 20;
        }
        return (int) Math.ceil(width * COST_SCALE);
    }

    private record CostRenderLine(ItemStack stack, String text, int color) {
    }
}
