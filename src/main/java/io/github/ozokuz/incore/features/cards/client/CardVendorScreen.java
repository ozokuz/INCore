package io.github.ozokuz.incore.features.cards.client;

import io.github.ozokuz.incore.features.cards.CardVendorService;
import io.github.ozokuz.incore.features.cards.network.CardNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class CardVendorScreen extends Screen {
    private final CardVendorService.VendorScreenData data;
    private int page;
    private static final int ROWS_PER_PAGE = 6;

    public CardVendorScreen(CardVendorService.VendorScreenData data) {
        super(Component.translatable("screen.incore.cards.vendor.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        rebuildVendorWidgets();
    }

    private void rebuildVendorWidgets() {
        clearWidgets();
        int left = this.width / 2 - 160;
        int top = 34;
        List<CardVendorService.VendorOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        page = Math.clamp(page, 0, maxPages - 1);
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        for (int i = start; i < end; i++) {
            CardVendorService.VendorOfferView offer = offers.get(i);
            int row = i - start;
            int y = top + row * 28;
            boolean affordable = data.tokenCount() >= offer.tokenCost() && data.spurCount() >= offer.spurCost();
            Button buyButton = this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.cards.vendor.buy"), button -> {
                        ResourceLocation offerId = ResourceLocation.tryParse(offer.id());
                        if (offerId != null) {
                            CardNetworking.sendVendorPurchase(offerId);
                        }
                    })
                    .bounds(left + 238, y + 4, 72, 18)
                    .build());
            buyButton.active = affordable;
        }

        this.addRenderableWidget(Button.builder(Component.literal("Prev"), button -> {
                    page = Math.max(0, page - 1);
                    rebuildVendorWidgets();
                })
                .bounds(left + 8, this.height - 52, 54, 18)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Next"), button -> {
                    page = Math.min(maxPages - 1, page + 1);
                    rebuildVendorWidgets();
                })
                .bounds(left + 66, this.height - 52, 54, 18)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int left = this.width / 2 - 160;
        int right = this.width / 2 + 160;
        int top = 18;
        int bottom = this.height - 40;

        guiGraphics.fill(left, top, right, bottom, 0xCC120E18);
        guiGraphics.fill(left, top, right, top + 1, 0xFF6CE0FF);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 6, 0xECF7FF);

        guiGraphics.drawString(
                this.font,
                Component.translatable("screen.incore.cards.vendor.balance", data.tokenCount(), data.spurCount()),
                left + 8,
                top + 18,
                0xA9E8FF,
                false
        );

        List<CardVendorService.VendorOfferView> offers = data.offers();
        int maxPages = Math.max(1, (int) Math.ceil(offers.size() / (double) ROWS_PER_PAGE));
        int start = page * ROWS_PER_PAGE;
        int end = Math.min(offers.size(), start + ROWS_PER_PAGE);

        guiGraphics.drawString(this.font, "Page " + (page + 1) + "/" + maxPages, right - 62, top + 20, 0xA9E8FF, false);
        int y = top + 36;
        for (int i = start; i < end; i++) {
            CardVendorService.VendorOfferView offer = offers.get(i);
            boolean affordable = data.tokenCount() >= offer.tokenCost() && data.spurCount() >= offer.spurCost();
            int panelColor = affordable ? 0x66233648 : 0x663A1D25;
            guiGraphics.fill(left + 6, y, right - 6, y + 24, panelColor);
            guiGraphics.fill(left + 6, y, right - 6, y + 1, affordable ? 0xFF66D9FF : 0xFFCE6D6D);
            String row = offer.name() + "  x" + offer.count() + "  (" + offer.productType() + ")";
            String cost = "TOKEN " + offer.tokenCost() + " + SPUR " + offer.spurCost();
            guiGraphics.drawString(this.font, row, left + 12, y + 4, 0xF0F0F0, false);
            guiGraphics.drawString(this.font, cost, left + 12, y + 14, affordable ? 0x89D6FF : 0xFF9F9F, false);
            y += 28;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
