package ozokuz.incore.client.features.vendingmachine;

import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.vendingmachine.VendingMachineCurrencyView;
import ozokuz.incore.features.vendingmachine.VendingMachineService;
import ozokuz.incore.features.vendingmachine.network.VendingMachineNetworking;
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

public class VendingMachineScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.VENDING_MACHINE;
    private static final int ROWS_PER_PAGE = 5;
    private static final int PANEL_WIDTH = 340;
    private static final int PANEL_TOP = 16;
    private static final int PANEL_BOTTOM_MARGIN = 36;
    private static final int ROW_HEIGHT = 48;
    private static final float COST_SCALE = 0.75F;

    private final VendingMachineService.VendingMachineScreenData data;
    private final Map<String, Integer> quantities = new HashMap<>();
    private int page;
    private Integer previousMenuBlur;

    public VendingMachineScreen(VendingMachineService.VendingMachineScreenData data) {
        super(Component.translatable("screen.incore.vending_machine.title"));
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
        rebuildVendingMachineWidgets();
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.previousMenuBlur != null) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.previousMenuBlur);
        }
        this.previousMenuBlur = null;
        super.removed();
    }

    private void rebuildVendingMachineWidgets() {
        clearWidgets();
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = this.width / 2 + PANEL_WIDTH / 2;
        int rowsTop = PANEL_TOP + 34;
        List<VendingMachineService.VendingMachineOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        page = Math.clamp(page, 0, maxPages - 1);
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            VendingMachineService.VendingMachineOfferView offer = offers.get(i);
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

            Button buyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.vending_machine.buy"), button -> onBuyOffer(offer))
                    .bounds(right - 74, y + 13, 64, 20)
                    .build());
            buyButton.active = isPurchasable(offer, quantity);
        }

        Button prevButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.vending_machine.prev"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildVendingMachineWidgets();
                })
                .bounds(left + 2, this.height - 60, 48, 20)
                .build());
        prevButton.active = page > 0;

        Button nextButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.vending_machine.next"), button -> {
                    page = Math.min(maxPages - 1, page + 1);
                    rebuildVendingMachineWidgets();
                })
                .bounds(left + 52, this.height - 60, 48, 20)
                .build());
        nextButton.active = page < maxPages - 1;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 40, this.height - 60, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ThemedUi ui = themed(guiGraphics);
        ui.drawBackdrop(this.width, this.height);
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = this.width / 2 + PANEL_WIDTH / 2;
        int top = PANEL_TOP;
        int bottom = this.height - PANEL_BOTTOM_MARGIN;

        ui.drawWindow(left, top, right - left, bottom - top);

        List<VendingMachineService.VendingMachineOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        int y = top + 34;
        for (int i = start; i < end; i++) {
            VendingMachineService.VendingMachineOfferView offer = offers.get(i);
            int quantity = selectedQuantity(offer);
            boolean soldOut = offer.stockRemaining() <= 0;
            boolean purchasable = isPurchasable(offer, quantity);
            int panelColor = soldOut ? UIScreenTheme.VendingMachine.LIST_ROW_FILL_SOLD_OUT : (purchasable ? UIScreenTheme.VendingMachine.LIST_ROW_FILL_AVAILABLE : UIScreenTheme.VendingMachine.LIST_ROW_FILL_BLOCKED);
            int headerColor = soldOut ? UIScreenTheme.VendingMachine.LIST_ROW_HEADER_SOLD_OUT : (purchasable ? UIScreenTheme.VendingMachine.LIST_ROW_HEADER_AVAILABLE : UIScreenTheme.VendingMachine.LIST_ROW_HEADER_BLOCKED);
            guiGraphics.fill(left + 6, y, right - 6, y + 44, panelColor);
            guiGraphics.fill(left + 6, y, right - 6, y + 1, headerColor);
            y += ROW_HEIGHT;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 6, UIScreenTheme.VendingMachine.TITLE_TEXT);
        renderBalancePanel(guiGraphics, left + 8, top + 16, left + 162, top + 30);
        renderVendingMachineModeBadge(guiGraphics, left + 166, top + 16, right - 64, top + 30);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.vending_machine.page", page + 1, maxPages), right - 56, top + 18, UIScreenTheme.VendingMachine.PAGE_TEXT, false);

        y = top + 34;
        for (int i = start; i < end; i++) {
            VendingMachineService.VendingMachineOfferView offer = offers.get(i);
            int quantity = selectedQuantity(offer);

            ItemStack preview = iconFromId(offer.previewItemId());
            if (!preview.isEmpty()) {
                guiGraphics.renderItem(preview, left + 10, y + 12);
            }

            guiGraphics.drawString(this.font, offer.name(), left + 48, y + 6, UIScreenTheme.VendingMachine.OFFER_NAME_TEXT, false);
            guiGraphics.drawString(this.font, Component.literal("x" + offer.count()), left + 32, y + 6, UIScreenTheme.VendingMachine.OFFER_COUNT_TEXT, false);
            renderDiscountBadge(guiGraphics, offer, right - 252, y + 2, right - 138);

            if (offer.stockRemaining() > 0) {
                guiGraphics.drawString(this.font, Component.translatable("screen.incore.vending_machine.stock", offer.stockRemaining()), left + 32, y + 21, UIScreenTheme.VendingMachine.STOCK_OK_TEXT, false);
            } else {
                guiGraphics.drawString(this.font, Component.translatable("screen.incore.vending_machine.sold_out"), left + 32, y + 21, UIScreenTheme.VendingMachine.STOCK_EMPTY_TEXT, false);
            }

            renderOfferCostPanel(guiGraphics, right - 252, y + 14, right - 138, y + 34, offer, quantity);
            int quantityX = right - 106;
            guiGraphics.drawCenteredString(this.font, Component.literal("x" + quantity), quantityX, y + 19, UIScreenTheme.VendingMachine.QUANTITY_TEXT);
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

    private void renderVendingMachineModeBadge(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        Component label;
        int fillColor;
        int textColor;
        if (data.darkMarket()) {
            label = Component.translatable("screen.incore.vending_machine.mode.dark_market");
            fillColor = UIScreenTheme.VendingMachine.STRESS_CHIP_FILL_BAD;
            textColor = UIScreenTheme.VendingMachine.STRESS_CHIP_TEXT_BAD;
        } else {
            String category = data.categoryId() == null ? "general" : displayCategoryLabel(data.categoryId());
            label = Component.translatable("screen.incore.vending_machine.mode.category", category);
            fillColor = UIScreenTheme.VendingMachine.STRESS_CHIP_FILL_OK;
            textColor = UIScreenTheme.VendingMachine.STRESS_CHIP_TEXT_OK;
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

    private void renderDiscountBadge(GuiGraphics guiGraphics, VendingMachineService.VendingMachineOfferView offer, int costLeft, int top, int costRight) {
        if (offer.discountPercent() <= 0 || offer.effectiveAmountPerUnit() >= offer.baseAmountPerUnit()) {
            return;
        }

        Component badgeText = Component.translatable("screen.incore.vending_machine.discount.badge", offer.discountPercent());
        int width = this.font.width(badgeText) + 8;
        int x = costLeft + Math.max(0, (costRight - costLeft - width) / 2);
        int fillColor = offer.curioOnlyDiscount() ? UIScreenTheme.VendingMachine.DISCOUNT_CHIP_FILL_CURIO : UIScreenTheme.VendingMachine.DISCOUNT_CHIP_FILL_DEFAULT;
        int textColor = offer.curioOnlyDiscount() ? UIScreenTheme.VendingMachine.DISCOUNT_CHIP_TEXT_CURIO : UIScreenTheme.VendingMachine.DISCOUNT_CHIP_TEXT_DEFAULT;
        guiGraphics.fill(x, top, x + width, top + 10, fillColor);
        guiGraphics.drawCenteredString(this.font, badgeText, x + width / 2, top + 1, textColor);
    }

    private void onBuyOffer(VendingMachineService.VendingMachineOfferView offer) {
        int quantity = selectedQuantity(offer);
        if (quantity <= 0) {
            return;
        }

        ResourceLocation offerId = ResourceLocation.tryParse(offer.id());
        if (offerId == null) {
            return;
        }

        if (canAffordByPrimary(offer, quantity)) {
            VendingMachineNetworking.sendVendingMachinePurchase(offerId, data.vending_machinePosLong(), quantity, false);
            return;
        }

        if (!canAffordByConversion(offer, quantity)) {
            return;
        }

        VendingMachineCurrencyView currency = offer.currency();
        Minecraft.getInstance().setScreen(new VendingMachineConversionConfirmScreen(
                this,
                offerId,
                offer.name(),
                data.vending_machinePosLong(),
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

    private void adjustQuantity(VendingMachineService.VendingMachineOfferView offer, int delta) {
        int max = maxSelectableQuantity(offer);
        if (max <= 0) {
            return;
        }

        int current = selectedQuantity(offer);
        int next = Math.clamp(current + delta, 1, max);
        quantities.put(offer.id(), next);
        rebuildVendingMachineWidgets();
    }

    private int selectedQuantity(VendingMachineService.VendingMachineOfferView offer) {
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

    private int maxSelectableQuantity(VendingMachineService.VendingMachineOfferView offer) {
        return Math.max(0, offer.stockRemaining());
    }

    private boolean hasConversion(VendingMachineService.VendingMachineOfferView offer) {
        VendingMachineCurrencyView currency = offer.currency();
        return currency.conversionRatio() > 0
                && currency.conversionTypeId() != null
                && !currency.conversionTypeId().isBlank()
                && !currency.conversionTypeId().equals(currency.typeId())
                && currency.conversionIconItemId() != null
                && !currency.conversionIconItemId().isBlank();
    }

    private int requiredPrimary(VendingMachineService.VendingMachineOfferView offer, int quantity) {
        long required = (long) offer.currency().amountPerUnit() * quantity;
        if (required > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) required);
    }

    private int primaryShortfall(VendingMachineService.VendingMachineOfferView offer, int quantity) {
        long shortfall = (long) requiredPrimary(offer, quantity) - Math.max(0, offer.currency().availablePrimary());
        if (shortfall > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) shortfall);
    }

    private int requiredConversion(VendingMachineService.VendingMachineOfferView offer, int quantity) {
        long required = (long) primaryShortfall(offer, quantity) * Math.max(0, offer.currency().conversionRatio());
        if (required > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.max(0, (int) required);
    }

    private boolean canAffordByPrimary(VendingMachineService.VendingMachineOfferView offer, int quantity) {
        return primaryShortfall(offer, quantity) <= 0;
    }

    private boolean canAffordByConversion(VendingMachineService.VendingMachineOfferView offer, int quantity) {
        int shortfall = primaryShortfall(offer, quantity);
        if (shortfall <= 0 || !hasConversion(offer)) {
            return false;
        }

        return requiredConversion(offer, quantity) <= Math.max(0, offer.currency().availableConversion());
    }

    private boolean isPurchasable(VendingMachineService.VendingMachineOfferView offer, int quantity) {
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
            VendingMachineService.VendingMachineOfferView offer,
            int quantity
    ) {
        List<CostRenderLine> lines = buildCostLines(offer, quantity);
        if (lines.isEmpty()) {
            return;
        }

        guiGraphics.fill(left, top, right, bottom, UIScreenTheme.VendingMachine.OVERLAY_FILL);

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
            VendingMachineService.VendingMachineOfferView offer,
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
        guiGraphics.drawString(this.font, originalText, textX, textY, UIScreenTheme.VendingMachine.STRIKE_TEXT, false);
        guiGraphics.fill(textX, textY + 4, textX + textWidth, textY + 5, UIScreenTheme.VendingMachine.STRIKE_LINE);
    }

    private List<CostRenderLine> buildCostLines(VendingMachineService.VendingMachineOfferView offer, int quantity) {
        List<CostRenderLine> lines = new ArrayList<>();
        if (quantity <= 0) {
            return lines;
        }

        VendingMachineCurrencyView currency = offer.currency();
        int requiredPrimary = requiredPrimary(offer, quantity);
        int availablePrimary = Math.max(0, currency.availablePrimary());
        int primaryCovered = Math.min(requiredPrimary, availablePrimary);
        int missingPrimary = Math.max(0, requiredPrimary - primaryCovered);
        boolean conversionAffordable = missingPrimary > 0 && canAffordByConversion(offer, quantity);

        ItemStack primaryIcon = iconFromId(currency.primaryIconItemId());
        if (primaryCovered > 0) {
            lines.add(new CostRenderLine(primaryIcon, "x" + primaryCovered, UIScreenTheme.VendingMachine.STOCK_OK_TEXT));
        }

        if (missingPrimary > 0) {
            if (!hasConversion(offer) || !conversionAffordable) {
                lines.add(new CostRenderLine(primaryIcon, "x" + missingPrimary, UIScreenTheme.VendingMachine.COST_MISSING_TEXT));
            }

            if (hasConversion(offer)) {
                int requiredConversion = requiredConversion(offer, quantity);
                ItemStack conversionIcon = iconFromId(currency.conversionIconItemId());
                lines.add(new CostRenderLine(conversionIcon, "x" + requiredConversion, conversionAffordable ? UIScreenTheme.VendingMachine.STOCK_OK_TEXT : UIScreenTheme.VendingMachine.COST_MISSING_TEXT));
            }
        }

        if (requiredPrimary <= 0) {
            lines.add(new CostRenderLine(ItemStack.EMPTY, Component.translatable("screen.incore.vending_machine.free").getString(), UIScreenTheme.VendingMachine.STOCK_OK_TEXT));
        }

        return lines;
    }

    private void renderBalancePanel(GuiGraphics guiGraphics, int left, int top, int right, int bottom) {
        List<CostRenderLine> lines = buildBalanceLines();
        if (lines.isEmpty()) {
            return;
        }

        guiGraphics.fill(left, top, right, bottom, UIScreenTheme.VendingMachine.OVERLAY_FILL);

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
        for (VendingMachineService.BalanceEntryView balance : data.balances()) {
            lines.add(new CostRenderLine(iconFromId(balance.iconItemId()), "x" + Math.max(0, balance.amount()), UIScreenTheme.VendingMachine.STOCK_OK_TEXT));
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
        themed(guiGraphics).drawScaledItemLine(line.stack(), line.text(), x, y, COST_SCALE, line.color());
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

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
