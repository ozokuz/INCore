package io.github.ozokuz.incore.client.features.market;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
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
import net.minecraft.world.item.Items;

public class MarketTradeConfirmScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.CONFIRMATION;
    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 190;
    private static final ResourceLocation SPUR_ICON_ITEM = ResourceLocation.parse("numismatics:spur");

    private final Screen parent;
    private final MarketService.ScreenData data;
    private final String selectedItemId;
    private final boolean isBuy;
    private int quantity = 1;
    private Integer previousMenuBlur;

    public MarketTradeConfirmScreen(
            Screen parent,
            MarketService.ScreenData data,
            String selectedItemId,
            boolean isBuy
    ) {
        super(Component.translatable(isBuy ? "screen.incore.market.confirm_buy.title" : "screen.incore.market.confirm_sell.title"));
        this.parent = parent;
        this.data = data;
        this.selectedItemId = selectedItemId;
        this.isBuy = isBuy;
    }

    @Override
    protected void init() {
        if (this.previousMenuBlur == null && this.minecraft != null) {
            this.previousMenuBlur = this.minecraft.options.getMenuBackgroundBlurriness();
            if (this.previousMenuBlur > 0) {
                this.minecraft.options.menuBackgroundBlurriness().set(0);
            }
        }
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        clearWidgets();

        int left = this.width / 2 - PANEL_WIDTH / 2;
        int top = this.height / 2 - PANEL_HEIGHT / 2;
        int buttonY = top + 154;

        MarketService.ItemView item = selectedItem();
        int maxQty = maxQuantity(item);
        quantity = Math.clamp(quantity, 1, Math.max(1, maxQty));

        Button minusButton = this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                    quantity = Math.max(1, quantity - 1);
                    rebuildWidgets();
                })
                .bounds(left + 24, buttonY, 20, 20)
                .build());
        minusButton.active = quantity > 1;

        this.addRenderableWidget(Button.builder(Component.literal("x" + quantity), button -> {})
                .bounds(left + 48, buttonY, 64, 20)
                .build());

        Button plusButton = this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    quantity = Math.min(maxQty, quantity + 1);
                    rebuildWidgets();
                })
                .bounds(left + 116, buttonY, 20, 20)
                .build());
        plusButton.active = quantity < maxQty;

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> this.minecraft.setScreen(parent))
                .bounds(left + 186, buttonY, 84, 20)
                .build());

        Button confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable(isBuy ? "screen.incore.market.confirm.buy" : "screen.incore.market.confirm.sell"),
                        button -> {
                            ResourceLocation itemId = MarketScreenDataUtil.parseItemId(selectedItemId);
                            if (itemId != null && data.terminalPos() != null) {
                                if (isBuy) {
                                    MarketNetworking.sendBuy(data.terminalPos(), itemId, quantity);
                                } else {
                                    MarketNetworking.sendSell(data.terminalPos(), itemId, quantity);
                                }
                            }
                            this.minecraft.setScreen(parent);
                        })
                .bounds(left + 278, buttonY, 120, 20)
                .build());
        confirmButton.active = canAfford(item);
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
        ThemedUi ui = themed(guiGraphics);
        ui.drawBackdrop(this.width, this.height);

        int left = this.width / 2 - PANEL_WIDTH / 2;
        int right = this.width / 2 + PANEL_WIDTH / 2;
        int top = this.height / 2 - PANEL_HEIGHT / 2;
        int bottom = this.height / 2 + PANEL_HEIGHT / 2;
        int exchangeBottom = top + 108;

        ui.drawWindow(left, top, PANEL_WIDTH, PANEL_HEIGHT);
        ui.drawRect(left, top, right, exchangeBottom, THEME.theme().panel().fill());
        ui.drawBorder(left, top, right, exchangeBottom, THEME.theme().panel().borderTop());

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        MarketService.ItemView item = selectedItem();
        if (item == null) {
            guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xF1F3F8);
            return;
        }

        int price = item.currentPriceSpur();
        int totalCost = quantity * price;
        int ownedItems = item.inventoryCount();
        int ownedSpur = data.balanceSpur();
        int stackUnitSize = stackUnitSizeForItem();
        int afterItems = isBuy ? ownedItems + (quantity * stackUnitSize) : ownedItems - (quantity * stackUnitSize);
        int afterSpur = isBuy ? ownedSpur - totalCost : ownedSpur + totalCost;

        int sourceCenterX = left + 112;
        int targetCenterX = right - 112;
        int amountY = top + 52;

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, top + 8, 0xF1F3F8);
        guiGraphics.drawCenteredString(
                this.font,
                Component.literal(item.displayName() + " x" + quantity),
                this.width / 2,
                top + 24,
                0xC9CDD6
        );

        ItemStack itemIcon = itemIconStack();
        ItemStack spurIcon = spurIconStack();

        if (isBuy) {
            guiGraphics.drawCenteredString(this.font, Component.literal("Spur"), sourceCenterX, top + 40, 0xD9DCE3);
            guiGraphics.drawCenteredString(this.font, Component.literal("Items"), targetCenterX, top + 40, 0xD9DCE3);
            drawScaledCenteredString(guiGraphics, Integer.toString(totalCost), sourceCenterX, amountY, 2.0F, 0xFFFFFF);
            drawScaledCenteredString(guiGraphics, "x" + quantity, targetCenterX, amountY, 2.0F, 0xFFFFFF);

            guiGraphics.renderItem(spurIcon, sourceCenterX + 30, amountY + 3);
            guiGraphics.renderItem(itemIcon, targetCenterX + 24, amountY + 3);

            drawChip(guiGraphics, sourceCenterX, top + 90, Component.translatable("screen.incore.market.confirm.balance", ownedSpur), 0xDD1D2127, 0xE6EDF9);
            drawChip(guiGraphics, targetCenterX, top + 90, Component.translatable("screen.incore.market.confirm.receive_items", quantity), 0xDD1D2127, 0xE6EDF9);
        } else {
            guiGraphics.drawCenteredString(this.font, Component.literal("Items"), sourceCenterX, top + 40, 0xD9DCE3);
            guiGraphics.drawCenteredString(this.font, Component.literal("Spur"), targetCenterX, top + 40, 0xD9DCE3);
            drawScaledCenteredString(guiGraphics, "x" + quantity, sourceCenterX, amountY, 2.0F, 0xFFFFFF);
            drawScaledCenteredString(guiGraphics, Integer.toString(totalCost), targetCenterX, amountY, 2.0F, 0xFFFFFF);

            guiGraphics.renderItem(itemIcon, sourceCenterX + 24, amountY + 3);
            guiGraphics.renderItem(spurIcon, targetCenterX + 30, amountY + 3);

            drawChip(guiGraphics, sourceCenterX, top + 90, Component.translatable("screen.incore.market.confirm.owned_items", ownedItems), 0xDD1D2127, 0xE6EDF9);
            drawChip(guiGraphics, targetCenterX, top + 90, Component.translatable("screen.incore.market.confirm.receive_spur", totalCost), 0xDD1D2127, 0xE6EDF9);
        }

        guiGraphics.drawCenteredString(this.font, Component.literal(">>"), this.width / 2, top + 60, 0xF5F5F5);

        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.market.confirm.rate_each", price),
                this.width / 2,
                exchangeBottom + 8,
                0xAAB2BF
        );

        if (isBuy) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.incore.market.confirm.after_spur", afterSpur),
                    this.width / 2,
                    exchangeBottom + 22,
                    afterSpur >= 0 ? 0xBDE8BD : 0xFF7777
            );
        } else {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.incore.market.confirm.after_items", afterItems),
                    this.width / 2,
                    exchangeBottom + 22,
                    afterItems >= 0 ? 0xBDE8BD : 0xFF7777
            );
        }
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

    private MarketService.ItemView selectedItem() {
        return MarketScreenDataUtil.findItem(data, selectedItemId);
    }

    private int maxQuantity(MarketService.ItemView item) {
        if (item == null) {
            return 1;
        }
        int price = item.currentPriceSpur();
        if (price <= 0) {
            return 64;
        }
        if (isBuy) {
            int maxByFunds = data.balanceSpur() / price;
            return Math.min(64, Math.max(1, maxByFunds));
        } else {
            int stackUnitSize = stackUnitSizeForItem();
            int maxByInventory = item.inventoryCount() / stackUnitSize;
            return Math.min(64, Math.max(1, maxByInventory));
        }
    }

    private boolean canAfford(MarketService.ItemView item) {
        if (item == null) {
            return false;
        }
        int price = item.currentPriceSpur();
        if (price <= 0) {
            return true;
        }
        if (isBuy) {
            return quantity * price <= data.balanceSpur();
        } else {
            int stackUnitSize = stackUnitSizeForItem();
            return quantity * stackUnitSize <= item.inventoryCount();
        }
    }

    private ItemStack spurIconStack() {
        Item item = BuiltInRegistries.ITEM.get(SPUR_ICON_ITEM);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private ItemStack itemIconStack() {
        ResourceLocation id = MarketScreenDataUtil.parseItemId(selectedItemId);
        if (id == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
    }

    private int stackUnitSizeForItem() {
        ResourceLocation id = MarketScreenDataUtil.parseItemId(selectedItemId);
        if (id == null) {
            return 64;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? 64 : Math.max(1, item.getDefaultMaxStackSize());
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
