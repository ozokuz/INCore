package io.github.ozokuz.incore.features.cards.client;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.cards.CardVendorService;
import io.github.ozokuz.incore.features.cards.network.CardNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class CardVendorSpurConversionConfirmScreen extends Screen {
    private static final ResourceLocation SPUR_ID = ResourceLocation.parse("numismatics:spur");
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 170;

    private final Screen parent;
    private final ResourceLocation offerId;
    private final String offerName;
    private final long vendorPosLong;
    private final int quantity;
    private final int missingTokens;
    private final int requiredSpur;
    private final int availableSpur;
    private Integer previousMenuBlur;

    public CardVendorSpurConversionConfirmScreen(
            Screen parent,
            ResourceLocation offerId,
            String offerName,
            long vendorPosLong,
            int quantity,
            int missingTokens,
            int requiredSpur,
            int availableSpur
    ) {
        super(Component.translatable("screen.incore.cards.vendor.conversion.title"));
        this.parent = parent;
        this.offerId = offerId;
        this.offerName = offerName;
        this.vendorPosLong = vendorPosLong;
        this.quantity = quantity;
        this.missingTokens = missingTokens;
        this.requiredSpur = requiredSpur;
        this.availableSpur = availableSpur;
    }

    @Override
    protected void init() {
        if (this.previousMenuBlur == null && this.minecraft != null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }

        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = this.height / 2 - PANEL_HEIGHT / 2;
        int buttonY = top + 134;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.minecraft.setScreen(parent))
                .bounds(left + 108, buttonY, 84, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.cards.vendor.conversion.confirm"), button -> {
                    this.minecraft.setScreen(parent);
                    CardNetworking.sendVendorPurchase(offerId, vendorPosLong, quantity, true);
                })
                .bounds(left + 198, buttonY, 120, 20)
                .build());
    }

    @Override
    public void removed() {
        if (this.minecraft != null && this.previousMenuBlur != null) {
            this.minecraft.options.menuBackgroundBlurriness().set(this.previousMenuBlur);
        }
        this.previousMenuBlur = null;
        super.removed();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = this.width / 2 + PANEL_WIDTH / 2;
        int top = this.height / 2 - PANEL_HEIGHT / 2;
        int bottom = this.height / 2 + PANEL_HEIGHT / 2;
        int exchangeBottom = top + 96;

        guiGraphics.fill(left, top, right, bottom, 0xE022252C);
        guiGraphics.fill(left, top, right, exchangeBottom, 0xEE3C4048);
        guiGraphics.fill(left, top, right, top + 1, 0xFF8F959F);
        guiGraphics.fill(left, exchangeBottom - 1, right, exchangeBottom, 0xFF8F959F);
        guiGraphics.fill(left, bottom - 1, right, bottom, 0xFF8F959F);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int sourceCenterX = left + 112;
        int targetCenterX = right - 112;
        int amountY = top + 42;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xF1F3F8);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.cards.vendor.conversion.offer", offerName + " x" + quantity),
                this.width / 2,
                top + 20,
                0xC9CDD6
        );

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.cards.vendor.conversion.from"),
                sourceCenterX,
                top + 32,
                0xD9DCE3
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.cards.vendor.conversion.to"),
                targetCenterX,
                top + 32,
                0xD9DCE3
        );
        drawScaledCenteredString(guiGraphics, Integer.toString(requiredSpur), sourceCenterX, amountY, 2.0F, 0xFFFFFF);
        drawScaledCenteredString(guiGraphics, Integer.toString(missingTokens), targetCenterX, amountY, 2.0F, 0xFFFFFF);
        guiGraphics.drawCenteredString(this.font, Component.literal(">>"), this.width / 2, top + 50, 0xF5F5F5);

        guiGraphics.renderItem(spurIcon(), sourceCenterX + 26, amountY + 3);
        guiGraphics.renderItem(new ItemStack(Registration.CARD_TOKEN_ITEM.get()), targetCenterX + 24, amountY + 3);

        drawChip(guiGraphics, sourceCenterX, top + 78, Component.translatable("screen.incore.cards.vendor.conversion.owned_spur", availableSpur), 0xDD1D2127, 0xE6EDF9);
        drawChip(guiGraphics, targetCenterX, top + 78, Component.translatable("screen.incore.cards.vendor.conversion.receive_token", missingTokens), 0xDD1D2127, 0xE6EDF9);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.cards.vendor.conversion.rate", CardVendorService.SPUR_PER_TOKEN),
                this.width / 2,
                exchangeBottom + 12,
                0xAAB2BF
        );
    }

    private void drawScaledCenteredString(GuiGraphics guiGraphics, String text, int centerX, int y, float scale, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        int textWidth = this.font.width(text);
        guiGraphics.drawString(this.font, text, -textWidth / 2, 0, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawChip(GuiGraphics guiGraphics, int centerX, int y, Component text, int fillColor, int textColor) {
        int textWidth = this.font.width(text);
        int width = textWidth + 14;
        int left = centerX - width / 2;
        guiGraphics.fill(left, y, left + width, y + 12, fillColor);
        guiGraphics.drawCenteredString(this.font, text, centerX, y + 2, textColor);
    }

    private ItemStack spurIcon() {
        Item spurItem = BuiltInRegistries.ITEM.get(SPUR_ID);
        return spurItem == Items.AIR ? ItemStack.EMPTY : spurItem.getDefaultInstance();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
