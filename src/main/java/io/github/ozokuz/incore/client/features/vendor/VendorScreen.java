package io.github.ozokuz.incore.client.features.vendor;

import io.github.ozokuz.incore.features.vendor.VendorCurrencyView;
import io.github.ozokuz.incore.features.vendor.VendorService;
import io.github.ozokuz.incore.features.vendor.network.VendorNetworking;
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

public class VendorScreen extends Screen {
    private static final int ROWS_PER_PAGE = 5;
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_TOP = 16;
    private static final int PANEL_BOTTOM_MARGIN = 36;
    private static final int ROW_HEIGHT = 48;
    private static final float COST_SCALE = 0.75F;

    private final VendorService.VendorScreenData data;
    private final Map<String, Integer> quantities = new HashMap<>();
    private int page;
    private Integer previousMenuBlur;

    public VendorScreen(VendorService.VendorScreenData data) {
        super(Component.translatable("screen.incore.vendor.title"));
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
        List<VendorService.VendorOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        page = Math.clamp(page, 0, maxPages - 1);
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            VendorService.VendorOfferView offer = offers.get(i);
            int row = i - start;
            int y = rowsTop + row * ROW_HEIGHT;
            int quantity = selectedQuantity(offer);

            Button minusButton = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> adjustQuantity(offer, -1))
                    .bounds(right - 132, y + 13, 16, 20)
                    .build());
            minusButton.active = quantity > 1;

            Button plusButton = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> adjustQuantity(offer, 1))
                    .bounds(right - 94, y + 13, 16, 20)
                    .build());
            plusButton.active = quantity > 0 && quantity < maxSelectableQuantity(offer);

            Button buyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.vendor.buy"), button -> onBuyOffer(offer))
                    .bounds(right - 74, y + 13, 64, 20)
                    .build());
            buyButton.active = isPurchasable(offer, quantity);
        }

        Button prevButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.vendor.prev"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildVendorWidgets();
                })
                .bounds(left + 8, this.height - 52, 58, 18)
                .build());
        prevButton.active = page > 0;

        Button nextButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.vendor.next"), button -> {
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

        List<VendorService.VendorOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        int y = top + 34;
        for (int i = start; i < end; i++) {
            VendorService.VendorOfferView offer = offers.get(i);
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
        renderBalancePanel(guiGraphics, left + 8, top + 16, left + 162, top + 30);
        renderVendorModeBadge(guiGraphics, left + 166, top + 16, right - 64, top + 30);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.vendor.page", page + 1, maxPages), right - 56, top + 18, 0xA9E8FF, false);

        y = top + 34;
        for (int i = start; i < end; i++) {
            VendorService.VendorOfferView offer = offers.get(i);
            int quantity = selectedQuantity(offer);

            ItemStack preview = iconFromId(offer.previewItemId());
            if (!preview.isEmpty()) {
                guiGraphics.renderItem(preview, left + 10, y + 12);
            }

            String typeLabel = typeLabel(offer.productType());
            guiGraphics.drawString(this.font, offer.name(), left + 32, y + 6, 0xF0F0F0, false);
            guiGraphics.drawString(this.font, Component.literal("x" + offer.count() + "  (" + typeLabel + ")"), left + 32, y + 18, 0xA2BFD8, false);
            renderDiscountBadge(guiGraphics, offer, right - 252, y + 2, right - 138);

            if (offer.stockRemaining() > 0) {
                guiGraphics.drawString(this.font, Component.translatable("screen.incore.vendor.stock", offer.stockRemaining()), left + 32, y + 30, 0xBDE8BD, false);
            } else {
                guiGraphics.drawString(this.font, Component.translatable("screen.incore.vendor.sold_out"), left + 32, y + 30, 0xFF7777, false);
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

    private String typeLabel(String productType) {
        ResourceLocation typeId = ResourceLocation.tryParse(productType);
        if (typeId == null) {
            return productType;
        }
        return typeId.getPath().toLowerCase(Locale.ROOT);
    }

    private void renderVendorModeBadge(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        Component label;
        int fillColor;
        int textColor;
        if (data.darkMarket()) {
            label = Component.translatable("screen.incore.vendor.mode.dark_market");
            fillColor = 0xAA472222;
            textColor = 0xFFE8A3A3;
        } else {
            String category = data.categoryId() == null ? "general" : displayCategoryLabel(data.categoryId());
            label = Component.translatable("screen.incore.vendor.mode.category", category);
            fillColor = 0xAA132739;
            textColor = 0xFF9EDCFF;
        }

        int desiredWidth = this.font.width(label) + 8;
        int width = Math.min(right - left, Math.max(34, desiredWidth));
        int x = left + Math.max(0, (right - left - width) / 2);
        guiGraphics.fill(x, top, x + width, bottom, fillColor);
        guiGraphics.drawCenteredString(this.font, label, x + width / 2, top + 3, textColor);
    }

    private String displayCategoryLabel(String categoryId) {
        ResourceLocation id = ResourceLocation.tryParse(categoryId);
        if (id == null) {
            return categoryId;
        }

        return formatCategoryText(id.getPath());
    }

    private String formatCategoryText(String rawPath) {
        String cleaned = rawPath.replace('/', ' ').replace('_', ' ').replace('-', ' ').trim();
        if (cleaned.isEmpty()) {
            return "general";
        }

        String[] parts = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.isEmpty()) {
                continue;
            }
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private void renderDiscountBadge(GuiGraphics guiGraphics, VendorService.VendorOfferView offer, int costLeft, int top, int costRight) {
        if (offer.discountPercent() <= 0 || offer.effectiveAmountPerUnit() >= offer.baseAmountPerUnit()) {
            return;
        }

        Component badgeText = Component.translatable("screen.incore.vendor.discount.badge", offer.discountPercent());
        int width = this.font.width(badgeText) + 8;
        int x = costLeft + Math.max(0, (costRight - costLeft - width) / 2);
        int fillColor = offer.curioOnlyDiscount() ? 0xCC0F3A32 : 0xCC3B2F10;
        int textColor = offer.curioOnlyDiscount() ? 0xFF84FFD7 : 0xFFF9CF7A;
        guiGraphics.fill(x, top, x + width, top + 10, fillColor);
        guiGraphics.drawCenteredString(this.font, badgeText, x + width / 2, top + 1, textColor);
    }

    private void onBuyOffer(VendorService.VendorOfferView offer) {
        int quantity = selectedQuantity(offer);
        if (quantity <= 0) {
            return;
        }

        ResourceLocation offerId = ResourceLocation.tryParse(offer.id());
        if (offerId == null) {
            return;
        }

        if (canAffordByPrimary(offer, quantity)) {
            VendorNetworking.sendVendorPurchase(offerId, data.vendorPosLong(), quantity, false);
            return;
        }

        if (!canAffordByConversion(offer, quantity)) {
            return;
        }

        VendorCurrencyView currency = offer.currency();
        Minecraft.getInstance().setScreen(new VendorConversionConfirmScreen(
                this,
                offerId,
                offer.name(),
                data.vendorPosLong(),
                quantity,
                primaryShortfall(offer, quantity),
                requiredConversion(offer, quantity),
                Math.max(0, currency.availableConversion()),
                safeLabel(currency.primaryLabel()),
                safeLabel(currency.conversionLabel()),
                currency.primaryIconItemId(),
                currency.conversionIconItemId()
        ));
    }

    private String safeLabel(String label) {
        return label == null || label.isBlank() ? "Currency" : label;
    }

    private void adjustQuantity(VendorService.VendorOfferView offer, int delta) {
        int max = maxSelectableQuantity(offer);
        if (max <= 0) {
            return;
        }

        int current = selectedQuantity(offer);
        int next = Math.clamp(current + delta, 1, max);
        quantities.put(offer.id(), next);
        rebuildVendorWidgets();
    }

    private int selectedQuantity(VendorService.VendorOfferView offer) {
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

    private int maxSelectableQuantity(VendorService.VendorOfferView offer) {
        return Math.max(0, offer.stockRemaining());
    }

    private boolean hasConversion(VendorService.VendorOfferView offer) {
        VendorCurrencyView currency = offer.currency();
        return currency.conversionRatio() > 0
                && currency.conversionTypeId() != null
                && !currency.conversionTypeId().isBlank()
                && !currency.conversionTypeId().equals(currency.typeId())
                && currency.conversionIconItemId() != null
                && !currency.conversionIconItemId().isBlank();
    }

    private int requiredPrimary(VendorService.VendorOfferView offer, int quantity) {
        long required = (long) offer.currency().amountPerUnit() * quantity;
        if (required > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) required);
    }

    private int primaryShortfall(VendorService.VendorOfferView offer, int quantity) {
        long shortfall = (long) requiredPrimary(offer, quantity) - Math.max(0, offer.currency().availablePrimary());
        if (shortfall > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) shortfall);
    }

    private int requiredConversion(VendorService.VendorOfferView offer, int quantity) {
        long required = (long) primaryShortfall(offer, quantity) * Math.max(0, offer.currency().conversionRatio());
        if (required > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) required);
    }

    private boolean canAffordByPrimary(VendorService.VendorOfferView offer, int quantity) {
        return primaryShortfall(offer, quantity) <= 0;
    }

    private boolean canAffordByConversion(VendorService.VendorOfferView offer, int quantity) {
        int shortfall = primaryShortfall(offer, quantity);
        if (shortfall <= 0 || !hasConversion(offer)) {
            return false;
        }

        return requiredConversion(offer, quantity) <= Math.max(0, offer.currency().availableConversion());
    }

    private boolean isPurchasable(VendorService.VendorOfferView offer, int quantity) {
        if (quantity <= 0) {
            return false;
        }

        return canAffordByPrimary(offer, quantity) || canAffordByConversion(offer, quantity);
    }

    private void renderOfferCostPanel(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int right,
            int bottom,
            VendorService.VendorOfferView offer,
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

        renderOriginalCostOverlay(guiGraphics, left, top, right, offer, quantity);
    }

    private void renderOriginalCostOverlay(
            GuiGraphics guiGraphics,
            int left,
            int top,
            int right,
            VendorService.VendorOfferView offer,
            int quantity
    ) {
        if (offer.discountPercent() <= 0 || quantity <= 0) {
            return;
        }
        if (offer.effectiveAmountPerUnit() >= offer.baseAmountPerUnit()) {
            return;
        }

        long baseTotalLong = (long) offer.baseAmountPerUnit() * quantity;
        if (baseTotalLong <= 0L) {
            return;
        }
        long baseTotal = Math.min(Integer.MAX_VALUE, baseTotalLong);
        String originalText = "x" + baseTotal;
        int textWidth = this.font.width(originalText);
        int textX = right - textWidth - 2;
        int textY = top - 9;
        guiGraphics.drawString(this.font, originalText, textX, textY, 0xAA9C9DA3, false);
        guiGraphics.fill(textX, textY + 4, textX + textWidth, textY + 5, 0xCC909197);
    }

    private List<CostRenderLine> buildCostLines(VendorService.VendorOfferView offer, int quantity) {
        List<CostRenderLine> lines = new ArrayList<>();
        if (quantity <= 0) {
            return lines;
        }

        VendorCurrencyView currency = offer.currency();
        int requiredPrimary = requiredPrimary(offer, quantity);
        int availablePrimary = Math.max(0, currency.availablePrimary());
        int primaryCovered = Math.min(requiredPrimary, availablePrimary);
        int missingPrimary = Math.max(0, requiredPrimary - primaryCovered);
        boolean conversionAffordable = missingPrimary > 0 && canAffordByConversion(offer, quantity);

        ItemStack primaryIcon = iconFromId(currency.primaryIconItemId());
        if (primaryCovered > 0) {
            lines.add(new CostRenderLine(primaryIcon, "x" + primaryCovered, 0xBDE8BD));
        }

        if (missingPrimary > 0) {
            if (!hasConversion(offer) || !conversionAffordable) {
                lines.add(new CostRenderLine(primaryIcon, "x" + missingPrimary, 0xFF5555));
            }

            if (hasConversion(offer)) {
                int requiredConversion = requiredConversion(offer, quantity);
                ItemStack conversionIcon = iconFromId(currency.conversionIconItemId());
                lines.add(new CostRenderLine(conversionIcon, "x" + requiredConversion, conversionAffordable ? 0xBDE8BD : 0xFF5555));
            }
        }

        if (requiredPrimary <= 0) {
            lines.add(new CostRenderLine(ItemStack.EMPTY, Component.translatable("screen.incore.vendor.free").getString(), 0xBDE8BD));
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
        for (VendorService.BalanceEntryView balance : data.balances()) {
            lines.add(new CostRenderLine(iconFromId(balance.iconItemId()), "x" + Math.max(0, balance.amount()), 0xBDE8BD));
        }
        return lines;
    }

    private ItemStack iconFromId(String itemIdString) {
        if (itemIdString == null || itemIdString.isBlank()) {
            return ItemStack.EMPTY;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
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
