package io.github.ozokuz.incore.features.shop.client;

import io.github.ozokuz.incore.features.shop.ShopService;
import io.github.ozokuz.incore.features.shop.network.ShopNetworking;
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

public class ShopSelectionScreen extends Screen implements ShopPayloadUpdatable {
    private static final int LEFT_MARGIN = 14;
    private static final int TOP = 36;
    private static final int BOTTOM_MARGIN = 30;
    private static final int CATEGORY_WIDTH = 130;
    private static final int GAP = 8;
    private static final int CATEGORY_ROW_HEIGHT = 24;
    private static final int OFFER_ROW_HEIGHT = 24;

    private ShopService.ScreenData data;
    private @Nullable String selectedCategoryId;
    private int offerScrollRow;

    public ShopSelectionScreen(String json) {
        this(ShopScreenDataUtil.parse(json), null, 0);
    }

    public ShopSelectionScreen(ShopService.ScreenData data, @Nullable String selectedCategoryId, int offerScrollRow) {
        super(Component.translatable("screen.incore.shop.title"));
        this.data = data;
        this.selectedCategoryId = selectedCategoryId;
        this.offerScrollRow = Math.max(0, offerScrollRow);
        ensureSelection();
    }

    @Override
    public void updatePayload(String json) {
        this.data = ShopScreenDataUtil.parse(json);
        ensureSelection();
        clampOfferScroll();
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width - 90, height - 26, 80, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.shop.refresh"), button ->
                        ShopNetworking.requestOpenShopScreen(
                                ShopScreenDataUtil.parseResource(selectedCategoryId),
                                null
                        ))
                .bounds(width - 186, 10, 82, 20)
                .build());

        clampOfferScroll();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = scrollY > 0 ? -1 : 1;
        offerScrollRow = Math.max(0, Math.min(offerScrollRow + delta, maxOfferScrollRows()));
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        if (clickCategory(mouseX, mouseY)) {
            return true;
        }

        ShopService.OfferView offer = clickedOffer(mouseX, mouseY);
        if (offer != null) {
            minecraft.setScreen(new ShopDetailsScreen(data, selectedCategoryId, offer.offerId(), offerScrollRow));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, title, width / 2, 14, 0xF2F2F2);
        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.shop.balance", data.balanceSpur()),
                LEFT_MARGIN,
                16,
                0x9AE29A,
                false
        );

        drawPanel(guiGraphics, categoryPanelX(), TOP - 2, CATEGORY_WIDTH + 4, panelHeight() + 4, 0xAA151920, 0xFF454F63);
        drawPanel(guiGraphics, offersPanelX(), TOP - 2, offersPanelWidth() + 4, panelHeight() + 4, 0xAA151920, 0xFF454F63);

        renderCategoryList(guiGraphics, mouseX, mouseY);
        renderOfferList(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void ensureSelection() {
        List<ShopService.CategoryView> categories = ShopScreenDataUtil.orderedCategories(data);
        if (categories.isEmpty()) {
            selectedCategoryId = "";
            return;
        }

        if (selectedCategoryId != null && !selectedCategoryId.isBlank()) {
            for (ShopService.CategoryView category : categories) {
                if (selectedCategoryId.equals(category.categoryId())) {
                    return;
                }
            }
        }

        selectedCategoryId = data.selectedCategoryId();
        if (selectedCategoryId != null && !selectedCategoryId.isBlank()) {
            for (ShopService.CategoryView category : categories) {
                if (selectedCategoryId.equals(category.categoryId())) {
                    return;
                }
            }
        }

        selectedCategoryId = categories.getFirst().categoryId();
    }

    private boolean clickCategory(double mouseX, double mouseY) {
        List<ShopService.CategoryView> categories = ShopScreenDataUtil.orderedCategories(data);
        int x = categoryPanelX();
        int y = TOP;

        for (ShopService.CategoryView category : categories) {
            if (mouseX >= x && mouseX < x + CATEGORY_WIDTH && mouseY >= y && mouseY < y + CATEGORY_ROW_HEIGHT) {
                selectedCategoryId = category.categoryId();
                offerScrollRow = 0;
                clampOfferScroll();
                return true;
            }
            y += CATEGORY_ROW_HEIGHT;
        }
        return false;
    }

    private @Nullable ShopService.OfferView clickedOffer(double mouseX, double mouseY) {
        List<ShopService.OfferView> offers = ShopScreenDataUtil.offersForCategory(data, selectedCategoryId);
        if (offers.isEmpty()) {
            return null;
        }

        int visibleRows = Math.max(1, panelHeight() / OFFER_ROW_HEIGHT);
        int startIndex = offerScrollRow;
        int x = offersPanelX();
        int y = TOP;

        for (int row = 0; row < visibleRows; row++) {
            int index = startIndex + row;
            if (index >= offers.size()) {
                break;
            }

            if (mouseX >= x && mouseX < x + offersPanelWidth() && mouseY >= y && mouseY < y + OFFER_ROW_HEIGHT) {
                return offers.get(index);
            }
            y += OFFER_ROW_HEIGHT;
        }

        return null;
    }

    private void renderCategoryList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ShopService.CategoryView> categories = ShopScreenDataUtil.orderedCategories(data);
        int x = categoryPanelX();
        int y = TOP;

        if (categories.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.shop.no_categories"), x + 4, y + 4, 0xDD8D8D, false);
            return;
        }

        for (ShopService.CategoryView category : categories) {
            boolean selected = category.categoryId().equals(selectedCategoryId);
            boolean hovered = mouseX >= x && mouseX < x + CATEGORY_WIDTH && mouseY >= y && mouseY < y + CATEGORY_ROW_HEIGHT;

            int borderColor = selected ? 0xFF89C9FF : 0xFF3D4558;
            int fillColor = selected ? 0xFF283446 : (hovered ? 0xFF202A37 : 0xFF1B212C);
            drawPanel(guiGraphics, x, y, CATEGORY_WIDTH, CATEGORY_ROW_HEIGHT, fillColor, borderColor);

            int nameColor = category.locked() ? 0xFF9A9A9A : 0xECF2FF;
            String name = font.plainSubstrByWidth(category.displayName(), CATEGORY_WIDTH - 10);
            guiGraphics.drawString(font, name, x + 5, y + 4, nameColor, false);

            String stockText = category.availableStock() < 0
                    ? Component.translatable("screen.incore.shop.stock.unlimited").getString()
                    : Component.translatable("screen.incore.shop.stock.remaining", category.availableStock()).getString();
            guiGraphics.drawString(font, font.plainSubstrByWidth(stockText, CATEGORY_WIDTH - 10), x + 5, y + 14, 0xB8C2D3, false);
            y += CATEGORY_ROW_HEIGHT;
        }
    }

    private void renderOfferList(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ShopService.OfferView> offers = ShopScreenDataUtil.offersForCategory(data, selectedCategoryId);
        if (offers.isEmpty()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.shop.no_offers"), offersPanelX() + 6, TOP + 6, 0xDD8D8D, false);
            return;
        }

        int visibleRows = Math.max(1, panelHeight() / OFFER_ROW_HEIGHT);
        int x = offersPanelX();
        int y = TOP;
        int startIndex = offerScrollRow;

        for (int row = 0; row < visibleRows; row++) {
            int index = startIndex + row;
            if (index >= offers.size()) {
                break;
            }

            ShopService.OfferView offer = offers.get(index);
            boolean hovered = mouseX >= x && mouseX < x + offersPanelWidth() && mouseY >= y && mouseY < y + OFFER_ROW_HEIGHT;
            int borderColor = hovered ? 0xFF79A9DF : 0xFF3D4558;
            int fillColor = hovered ? 0xFF202A37 : 0xFF1B212C;
            drawPanel(guiGraphics, x, y, offersPanelWidth(), OFFER_ROW_HEIGHT, fillColor, borderColor);

            renderOfferIcon(guiGraphics, offer, x + 3, y + 4);

            int textColor = offer.locked() ? 0xFF9A9A9A : 0xECF2FF;
            guiGraphics.drawString(font, font.plainSubstrByWidth(offer.displayName(), offersPanelWidth() - 190), x + 23, y + 4, textColor, false);
            guiGraphics.drawString(font, Component.literal(offer.priceSpur() + " spur"), x + offersPanelWidth() - 116, y + 4, 0xCFE4FF, false);

            String stock = offer.availableStock() < 0
                    ? Component.translatable("screen.incore.shop.stock.unlimited").getString()
                    : Component.translatable("screen.incore.shop.stock.remaining", offer.availableStock()).getString();
            guiGraphics.drawString(font, font.plainSubstrByWidth(stock, 86), x + offersPanelWidth() - 116, y + 14, 0xB7C1D0, false);

            if (offer.locked()) {
                guiGraphics.drawString(font, Component.translatable("screen.incore.shop.locked_short"), x + 23, y + 14, 0xFF8A8A, false);
            } else {
                guiGraphics.drawString(font, Component.translatable("screen.incore.shop.bundle", offer.itemCount()), x + 23, y + 14, 0xB7C1D0, false);
            }
            y += OFFER_ROW_HEIGHT;
        }

        int totalRows = offers.size();
        int currentRow = Math.min(totalRows, offerScrollRow + 1);
        guiGraphics.drawString(
                font,
                Component.literal(currentRow + "/" + Math.max(1, totalRows)),
                width - 52,
                height - 38,
                0xB7C1D0,
                false
        );
    }

    private void renderOfferIcon(GuiGraphics guiGraphics, ShopService.OfferView offer, int x, int y) {
        ResourceLocation id = ShopScreenDataUtil.parseResource(offer.itemId());
        if (id == null) {
            return;
        }

        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || item == Items.AIR) {
            return;
        }

        guiGraphics.renderItem(new ItemStack(item), x, y);
    }

    private int categoryPanelX() {
        return LEFT_MARGIN;
    }

    private int offersPanelX() {
        return LEFT_MARGIN + CATEGORY_WIDTH + GAP;
    }

    private int offersPanelWidth() {
        return width - offersPanelX() - LEFT_MARGIN;
    }

    private int panelHeight() {
        return height - TOP - BOTTOM_MARGIN;
    }

    private int maxOfferScrollRows() {
        List<ShopService.OfferView> offers = ShopScreenDataUtil.offersForCategory(data, selectedCategoryId);
        int visibleRows = Math.max(1, panelHeight() / OFFER_ROW_HEIGHT);
        return Math.max(0, offers.size() - visibleRows);
    }

    private void clampOfferScroll() {
        offerScrollRow = Math.max(0, Math.min(offerScrollRow, maxOfferScrollRows()));
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
}
