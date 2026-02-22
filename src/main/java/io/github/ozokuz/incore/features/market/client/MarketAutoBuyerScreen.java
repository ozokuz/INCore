package io.github.ozokuz.incore.features.market.client;

import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerMenu;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MarketAutoBuyerScreen extends AbstractContainerScreen<MarketAutoBuyerMenu> {
    private static final int TEXT_COLOR = 0xCDD3DE;
    private static final int GHOST_SLOT_X = 74;
    private static final int GHOST_SLOT_Y = 66;

    private Button enabledButton;
    private @Nullable ResourceLocation ghostTargetItemId;
    private ItemStack ghostTargetPreview = ItemStack.EMPTY;

    public MarketAutoBuyerScreen(MarketAutoBuyerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 276;
    }

    @Override
    protected void init() {
        super.init();

        this.syncGhostTargetFromString(menu.targetItemId());

        this.enabledButton = this.addRenderableWidget(Button.builder(Component.literal(""), button -> {
                    sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize(), !menu.enabled());
                }).bounds(leftPos + 12, topPos + 86, 56, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Cap -"), b -> sendConfigUpdate(
                        currentTargetIdString(),
                        Math.max(1, menu.priceCap() - 1),
                        menu.batchSize(),
                        menu.enabled()
                )).bounds(leftPos + 74, topPos + 86, 38, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Cap +"), b -> sendConfigUpdate(
                        currentTargetIdString(),
                        menu.priceCap() + 1,
                        menu.batchSize(),
                        menu.enabled()
                )).bounds(leftPos + 114, topPos + 86, 38, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("Clear"), b -> {
                    clearGhostTarget();
                    sendConfigUpdate(
                            currentTargetIdString(),
                            menu.priceCap(),
                            menu.batchSize(),
                            menu.enabled()
                    );
                }).bounds(leftPos + 12, topPos + 108, 56, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Stk -"), b -> sendConfigUpdate(
                        currentTargetIdString(),
                        menu.priceCap(),
                        Math.max(1, menu.batchSize() - 1),
                        menu.enabled()
                )).bounds(leftPos + 74, topPos + 108, 38, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.literal("Stk +"), b -> sendConfigUpdate(
                        currentTargetIdString(),
                        menu.priceCap(),
                        Math.min(64, menu.batchSize() + 1),
                        menu.enabled()
                )).bounds(leftPos + 114, topPos + 108, 38, 20)
                .build());

        refreshEnabledButton();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        String menuTargetId = menu.targetItemId();
        if (ghostTargetItemId == null && menuTargetId != null && !menuTargetId.isBlank()) {
            syncGhostTargetFromString(menuTargetId);
        }
        refreshEnabledButton();
    }

    private void refreshEnabledButton() {
        if (enabledButton != null) {
            enabledButton.setMessage(Component.literal(menu.enabled() ? "On" : "Off"));
        }
    }

    private String currentTargetIdString() {
        return ghostTargetItemId == null ? "" : ghostTargetItemId.toString();
    }

    private void sendConfigUpdate(String targetItemId, int priceCap, int batchSize, boolean enabled) {
        MarketNetworking.sendAutoBuyerConfig(
                menu.positionAccessor().asLong(),
                targetItemId,
                priceCap,
                batchSize,
                enabled
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        drawPanel(guiGraphics, x, y, imageWidth, imageHeight, 0xFF13161A, 0xFF4A4F5A);
        drawPanel(guiGraphics, x + 5, y + 5, imageWidth - 10, 14, 0xFF20252C, 0xFF3D4350);
        drawPanel(guiGraphics, x + 8, y + 24, imageWidth - 16, 28, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + 8, y + 62, imageWidth - 16, 66, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + 8, y + 130, imageWidth - 16, 60, 0xFF1A1F26, 0xFF363D49);
        drawPanel(guiGraphics, x + 8, y + 188, imageWidth - 16, 82, 0xFF1A1F26, 0xFF363D49);

        int progressX = x + 12;
        int progressY = y + 55;
        int progressWidth = 180;
        drawPanel(guiGraphics, progressX - 1, progressY - 1, progressWidth + 2, 8, 0xFF101318, 0xFF2F3540);
        guiGraphics.fill(progressX, progressY, progressX + progressWidth, progressY + 6, 0xFF242B34);
        guiGraphics.fill(progressX, progressY, progressX + menu.progressScaled(progressWidth), progressY + 6, 0xFF71C2FF);

        drawSlotFrame(guiGraphics, x + GHOST_SLOT_X, y + GHOST_SLOT_Y);
        if (!ghostTargetPreview.isEmpty()) {
            guiGraphics.renderItem(ghostTargetPreview, x + GHOST_SLOT_X, y + GHOST_SLOT_Y);
        }
        if (isMouseOverGhostSlot(mouseX, mouseY)) {
            guiGraphics.fill(x + GHOST_SLOT_X, y + GHOST_SLOT_Y, x + GHOST_SLOT_X + 16, y + GHOST_SLOT_Y + 16, 0x55FFFFFF);
        }

        drawSlotFrame(guiGraphics, x + MarketAutoBuyerMenu.CARD_X, y + MarketAutoBuyerMenu.CARD_Y);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiGraphics,
                        x + MarketAutoBuyerMenu.OUTPUT_X + col * 18,
                        y + MarketAutoBuyerMenu.OUTPUT_Y + row * 18
                );
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(
                        guiGraphics,
                        x + MarketAutoBuyerMenu.PLAYER_INVENTORY_X + col * 18,
                        y + MarketAutoBuyerMenu.PLAYER_INVENTORY_Y + row * 18
                );
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(
                    guiGraphics,
                    x + MarketAutoBuyerMenu.PLAYER_INVENTORY_X + col * 18,
                    y + MarketAutoBuyerMenu.HOTBAR_Y
            );
        }

        int statusColor = switch (menu.status()) {
            case MarketAutoBuyerBlockEntity.STATUS_READY -> 0xFF7DD6A7;
            case MarketAutoBuyerBlockEntity.STATUS_DISABLED -> 0xFFAAAAAA;
            case MarketAutoBuyerBlockEntity.STATUS_PRICE_TOO_HIGH, MarketAutoBuyerBlockEntity.STATUS_NO_FUNDS -> 0xFFD17C7C;
            default -> 0xFFE2C777;
        };
        guiGraphics.fill(x + 198, y + 31, x + 206, y + 39, statusColor);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, 11, 9, 0xE6EBF4, false);
        guiGraphics.drawString(this.font, Component.literal("Status"), 12, 30, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 12, 192, TEXT_COLOR, false);

        Component status = switch (menu.status()) {
            case MarketAutoBuyerBlockEntity.STATUS_DISABLED -> Component.translatable("screen.incore.market.autobuyer.status.disabled");
            case MarketAutoBuyerBlockEntity.STATUS_NO_CARD -> Component.translatable("screen.incore.market.autobuyer.status.no_card");
            case MarketAutoBuyerBlockEntity.STATUS_NO_TARGET -> Component.translatable("screen.incore.market.autobuyer.status.no_target");
            case MarketAutoBuyerBlockEntity.STATUS_PRICE_TOO_HIGH -> Component.translatable("screen.incore.market.autobuyer.status.price_too_high");
            case MarketAutoBuyerBlockEntity.STATUS_NO_FUNDS -> Component.translatable("screen.incore.market.autobuyer.status.no_funds");
            case MarketAutoBuyerBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("screen.incore.market.autobuyer.status.output_full");
            default -> Component.translatable("screen.incore.market.autobuyer.status.ready");
        };
        List<FormattedCharSequence> statusLines = font.split(status, 136);
        for (int i = 0; i < statusLines.size() && i < 2; i++) {
            guiGraphics.drawString(this.font, statusLines.get(i), 56, 30 + i * this.font.lineHeight, TEXT_COLOR);
        }

        guiGraphics.drawString(this.font, Component.translatable("screen.incore.market.autobuyer.target"), 12, 70, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.market.autobuyer.target_hint"), 94, 70, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal("Cap: " + menu.priceCap()), 158, 92, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal("Stacks: " + menu.batchSize()), 158, 114, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal("Card"), 12, 132, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal("Output"), 46, 132, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (isMouseOverGhostSlot(mouseX, mouseY) && !ghostTargetPreview.isEmpty()) {
            guiGraphics.renderTooltip(this.font, ghostTargetPreview, mouseX, mouseY);
            return;
        }
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverGhostSlot(mouseX, mouseY)) {
            if (button == 1 || button == 2) {
                clearGhostTarget();
                sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize(), menu.enabled());
                return true;
            }

            if (button == 0) {
                ItemStack carried = menu.getCarried();
                if (!carried.isEmpty() && setGhostTargetFromItemStack(carried)) {
                    sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize(), menu.enabled());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean applyGhostTargetFromItemStack(ItemStack stack) {
        if (!setGhostTargetFromItemStack(stack)) {
            return false;
        }
        sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize(), menu.enabled());
        return true;
    }

    public void clearGhostTargetFromExternal() {
        clearGhostTarget();
        sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize(), menu.enabled());
    }

    public int ghostSlotLeft() {
        return leftPos + GHOST_SLOT_X - 1;
    }

    public int ghostSlotTop() {
        return topPos + GHOST_SLOT_Y - 1;
    }

    private boolean isMouseOverGhostSlot(double mouseX, double mouseY) {
        return mouseX >= leftPos + GHOST_SLOT_X
                && mouseX <= leftPos + GHOST_SLOT_X + 16
                && mouseY >= topPos + GHOST_SLOT_Y
                && mouseY <= topPos + GHOST_SLOT_Y + 16;
    }

    private void syncGhostTargetFromString(String targetId) {
        if (targetId == null || targetId.isBlank()) {
            clearGhostTarget();
            return;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(targetId.trim());
        if (itemId == null) {
            clearGhostTarget();
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            clearGhostTarget();
            return;
        }
        this.ghostTargetItemId = itemId;
        this.ghostTargetPreview = new ItemStack(item);
    }

    private boolean setGhostTargetFromItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        if (itemId == null || item == net.minecraft.world.item.Items.AIR) {
            return false;
        }

        this.ghostTargetItemId = itemId;
        this.ghostTargetPreview = new ItemStack(item);
        return true;
    }

    private void clearGhostTarget() {
        this.ghostTargetItemId = null;
        this.ghostTargetPreview = ItemStack.EMPTY;
    }

    private static void drawPanel(GuiGraphics guiGraphics, int x, int y, int width, int height, int fillColor, int borderColor) {
        guiGraphics.fill(x, y, x + width, y + height, fillColor);
        guiGraphics.fill(x, y, x + width, y + 1, borderColor);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, borderColor);
        guiGraphics.fill(x, y, x + 1, y + height, borderColor);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        drawPanel(guiGraphics, x - 1, y - 1, 18, 18, 0xFF252A32, 0xFF4A5261);
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF181D24);
    }
}
