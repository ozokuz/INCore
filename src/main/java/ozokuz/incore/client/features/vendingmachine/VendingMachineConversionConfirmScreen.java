package ozokuz.incore.client.features.vendingmachine;

import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.vendingmachine.network.VendingMachineNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class VendingMachineConversionConfirmScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.CONFIRMATION;
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 170;

    private final Screen parent;
    private final ResourceLocation offerId;
    private final String offerName;
    private final long vending_machinePosLong;
    private final int quantity;
    private final int missingPrimary;
    private final int requiredConversion;
    private final int availableConversion;
    private final String primaryLabel;
    private final String conversionLabel;
    private final String primaryIconItemId;
    private final String conversionIconItemId;
    private Integer previousMenuBlur;

    public VendingMachineConversionConfirmScreen(
            Screen parent,
            ResourceLocation offerId,
            String offerName,
            long vending_machinePosLong,
            int quantity,
            int missingPrimary,
            int requiredConversion,
            int availableConversion,
            String primaryLabel,
            String conversionLabel,
            String primaryIconItemId,
            String conversionIconItemId
    ) {
        super(Component.translatable("screen.incore.vending_machine.conversion.title"));
        this.parent = parent;
        this.offerId = offerId;
        this.offerName = offerName;
        this.vending_machinePosLong = vending_machinePosLong;
        this.quantity = quantity;
        this.missingPrimary = missingPrimary;
        this.requiredConversion = requiredConversion;
        this.availableConversion = availableConversion;
        this.primaryLabel = primaryLabel;
        this.conversionLabel = conversionLabel;
        this.primaryIconItemId = primaryIconItemId;
        this.conversionIconItemId = conversionIconItemId;
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
        this.addRenderableWidget(Button.builder(Component.translatable("screen.incore.vending_machine.conversion.confirm"), button -> {
                    this.minecraft.setScreen(parent);
                    VendingMachineNetworking.sendVendingMachinePurchase(offerId, vending_machinePosLong, quantity, true);
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
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = this.width / 2 + PANEL_WIDTH / 2;
        int top = this.height / 2 - PANEL_HEIGHT / 2;
        int bottom = this.height / 2 + PANEL_HEIGHT / 2;
        int exchangeBottom = top + 96;

        ThemedUi ui = themed(guiGraphics);
        ui.drawWindow(left, top, PANEL_WIDTH, PANEL_HEIGHT);
        ui.drawRect(left, top, right, exchangeBottom, THEME.theme().panel().fill());
        ui.drawBorder(left, top, right, exchangeBottom, THEME.theme().panel().borderTop());
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int sourceCenterX = left + 112;
        int targetCenterX = right - 112;
        int amountY = top + 42;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, UIScreenTheme.Confirmation.TITLE_TEXT);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.vending_machine.conversion.offer", offerName + " x" + quantity),
                this.width / 2,
                top + 20,
                UIScreenTheme.Confirmation.BODY_MUTED_TEXT
        );

        guiGraphics.drawCenteredString(this.font, Component.literal(conversionLabel), sourceCenterX, top + 32, UIScreenTheme.Confirmation.LABEL_TEXT);
        guiGraphics.drawCenteredString(this.font, Component.literal(primaryLabel), targetCenterX, top + 32, UIScreenTheme.Confirmation.LABEL_TEXT);
        drawScaledCenteredString(guiGraphics, Integer.toString(requiredConversion), sourceCenterX, amountY + 4, 2.0F, UIScreenTheme.Confirmation.VALUE_TEXT);
        drawScaledCenteredString(guiGraphics, Integer.toString(missingPrimary), targetCenterX, amountY + 4, 2.0F, UIScreenTheme.Confirmation.VALUE_TEXT);
        guiGraphics.drawCenteredString(this.font, Component.literal(">>"), this.width / 2, top + 50, UIScreenTheme.Confirmation.ARROW_TEXT);

        guiGraphics.renderItem(iconFromId(conversionIconItemId), sourceCenterX + 26, amountY + 3);
        guiGraphics.renderItem(iconFromId(primaryIconItemId), targetCenterX + 24, amountY + 3);

        drawChip(guiGraphics, sourceCenterX, top + 78, Component.translatable("screen.incore.vending_machine.conversion.owned", availableConversion), UIScreenTheme.Confirmation.CHIP_FILL, UIScreenTheme.Confirmation.CHIP_TEXT);
        drawChip(guiGraphics, targetCenterX, top + 78, Component.translatable("screen.incore.vending_machine.conversion.receive", missingPrimary), UIScreenTheme.Confirmation.CHIP_FILL, UIScreenTheme.Confirmation.CHIP_TEXT);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.vending_machine.conversion.rate", requiredConversion, missingPrimary),
                this.width / 2,
                exchangeBottom + 12,
                UIScreenTheme.Confirmation.DELTA_MUTED_TEXT
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
        themed(guiGraphics).drawChipCentered(centerX, y, text, fillColor, textColor);
    }

    private ItemStack iconFromId(String itemIdString) {
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdString);
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
