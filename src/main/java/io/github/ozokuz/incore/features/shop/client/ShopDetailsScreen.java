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

public class ShopDetailsScreen extends Screen implements ShopPayloadUpdatable {
    private final int returnScrollRow;

    private ShopService.ScreenData data;
    private @Nullable String selectedCategoryId;
    private @Nullable String selectedOfferId;
    private int quantity = 1;

    public ShopDetailsScreen(
            ShopService.ScreenData data,
            @Nullable String selectedCategoryId,
            @Nullable String selectedOfferId,
            int returnScrollRow
    ) {
        super(Component.translatable("screen.incore.shop.details.title"));
        this.data = data;
        this.selectedCategoryId = selectedCategoryId;
        this.selectedOfferId = selectedOfferId;
        this.returnScrollRow = Math.max(0, returnScrollRow);
        ensureSelection();
    }

    @Override
    public void updatePayload(String json) {
        this.data = ShopScreenDataUtil.parse(json);
        ensureSelection();
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

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.shop.back"), button ->
                        minecraft.setScreen(new ShopSelectionScreen(data, selectedCategoryId, returnScrollRow)))
                .bounds(16, 14, 60, 20)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("screen.incore.shop.refresh"), button ->
                        ShopNetworking.requestOpenShopScreen(
                                ShopScreenDataUtil.parseResource(selectedCategoryId),
                                ShopScreenDataUtil.parseResource(selectedOfferId)
                        ))
                .bounds(width - 98, 14, 82, 20)
                .build());

        ShopService.OfferView offer = selectedOffer();
        if (offer == null) {
            return;
        }

        addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                    quantity = Math.max(1, quantity - 1);
                    rebuildWidgets();
                }).bounds(16, height - 26, 20, 20)
                .build());

        addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    quantity = Math.min(64, quantity + 1);
                    rebuildWidgets();
                }).bounds(140, height - 26, 20, 20)
                .build());

        Button purchaseButton = Button.builder(Component.translatable("screen.incore.shop.purchase"), button -> {
                    ResourceLocation offerId = ShopScreenDataUtil.parseResource(selectedOfferId);
                    ResourceLocation categoryId = ShopScreenDataUtil.parseResource(selectedCategoryId);
                    if (offerId != null) {
                        ShopNetworking.sendPurchase(offerId, quantity, categoryId);
                    }
                }).bounds(width - 116, height - 26, 100, 20)
                .build();
        purchaseButton.active = !offer.locked() && (offer.availableStock() < 0 || offer.availableStock() > 0);
        addRenderableWidget(purchaseButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(font, title, width / 2, 16, 0xF2F2F2);
        guiGraphics.drawString(
                font,
                Component.translatable("screen.incore.shop.balance", data.balanceSpur()),
                84,
                20,
                0x9AE29A,
                false
        );

        ShopService.OfferView offer = selectedOffer();
        if (offer == null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.shop.no_offer_selected"), 16, 44, 0xDD8D8D, false);
            return;
        }

        ShopService.CategoryView category = ShopScreenDataUtil.findCategory(data, offer.categoryId());

        drawPanel(guiGraphics, 12, 40, width - 24, 120, 0xAA1B212B, 0xFF475063);
        drawPanel(guiGraphics, 12, 166, width - 24, Math.max(70, height - 204), 0xAA1B212B, 0xFF475063);

        renderOfferIcon(guiGraphics, offer, 18, 52);
        guiGraphics.drawString(font, Component.literal(offer.displayName()), 42, 48, offer.locked() ? 0xFF9A9A9A : 0xFFFFFF, false);

        if (category != null) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.shop.category", category.displayName()), 42, 62, 0xCFE4FF, false);
        }

        guiGraphics.drawString(font, Component.translatable("screen.incore.shop.price_each", offer.priceSpur()), 42, 74, 0xCFE4FF, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.shop.bundle", offer.itemCount()), 42, 86, 0xD0D0D0, false);

        String stockText = offer.availableStock() < 0
                ? Component.translatable("screen.incore.shop.stock.unlimited").getString()
                : Component.translatable("screen.incore.shop.stock.remaining", offer.availableStock()).getString();
        guiGraphics.drawString(font, Component.literal(stockText), 42, 98, 0xD0D0D0, false);

        long totalCost = (long) quantity * offer.priceSpur();
        guiGraphics.drawString(font, Component.translatable("screen.incore.shop.quantity", quantity), 42, 110, 0xD0D0D0, false);
        guiGraphics.drawString(font, Component.translatable("screen.incore.shop.total_cost", totalCost), 42, 122, 0xD0D0D0, false);

        if (offer.locked()) {
            guiGraphics.drawString(font, Component.translatable("screen.incore.shop.locked"), 42, 136, 0xFF8A8A, false);
        }

        guiGraphics.drawString(font, Component.translatable("screen.incore.shop.details_hint"), 16, 172, 0xB8C2D3, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(new ShopSelectionScreen(data, selectedCategoryId, returnScrollRow));
            return;
        }
        super.onClose();
    }

    private void ensureSelection() {
        if ((selectedCategoryId == null || selectedCategoryId.isBlank()) && data.selectedCategoryId() != null) {
            selectedCategoryId = data.selectedCategoryId();
        }

        if ((selectedOfferId == null || selectedOfferId.isBlank()) && data.selectedOfferId() != null) {
            selectedOfferId = data.selectedOfferId();
        }

        if (selectedOffer() != null) {
            return;
        }

        ShopService.OfferView fallback = null;
        for (ShopService.OfferView offer : data.offers()) {
            if (selectedCategoryId != null && !selectedCategoryId.isBlank() && selectedCategoryId.equals(offer.categoryId())) {
                fallback = offer;
                break;
            }
            if (fallback == null) {
                fallback = offer;
            }
        }

        if (fallback != null) {
            selectedOfferId = fallback.offerId();
            selectedCategoryId = fallback.categoryId();
        }
    }

    private @Nullable ShopService.OfferView selectedOffer() {
        return ShopScreenDataUtil.findOffer(data, selectedOfferId);
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

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }
}
