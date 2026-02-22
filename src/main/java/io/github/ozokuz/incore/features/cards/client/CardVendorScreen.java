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

    public CardVendorScreen(CardVendorService.VendorScreenData data) {
        super(Component.translatable("screen.incore.cards.vendor.title"));
        this.data = data;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 160;
        int top = 34;
        int maxRows = Math.min(8, data.offers().size());

        for (int i = 0; i < maxRows; i++) {
            CardVendorService.VendorOfferView offer = data.offers().get(i);
            int y = top + i * 24;
            this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.cards.vendor.buy"), button -> {
                        ResourceLocation offerId = ResourceLocation.tryParse(offer.id());
                        if (offerId != null) {
                            CardNetworking.sendVendorPurchase(offerId);
                        }
                    })
                    .bounds(left + 240, y + 2, 70, 18)
                    .build());
        }

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
        int top = 20;
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
        int rows = Math.min(8, offers.size());
        int y = top + 34;
        for (int i = 0; i < rows; i++) {
            CardVendorService.VendorOfferView offer = offers.get(i);
            guiGraphics.fill(left + 6, y, right - 6, y + 20, 0x661C2430);
            String row = offer.name() + " x" + offer.count() + " [" + offer.productType() + "]";
            String cost = "TOKEN " + offer.tokenCost() + " + SPUR " + offer.spurCost();
            guiGraphics.drawString(this.font, row, left + 10, y + 3, 0xF0F0F0, false);
            guiGraphics.drawString(this.font, cost, left + 10, y + 12, 0x9FD7FF, false);
            y += 24;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
