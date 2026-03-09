package io.github.ozokuz.incore.client.features.market;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedButton;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.market.content.MarketAutoTraderBlockEntity;
import io.github.ozokuz.incore.features.market.content.MarketAutoTraderMenu;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import net.minecraft.client.gui.GuiGraphics;
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

public class MarketAutoTraderScreen extends AbstractContainerScreen<MarketAutoTraderMenu> {
    private static final UIScreenTheme THEME = UIScreenTheme.MACHINE;
    private static final int TEXT_COLOR = THEME.theme().text().secondary();
    private static final int ACCENT_TEXT = UIScreenTheme.Machine.TITLE_TEXT;

    // Ghost slot position (within the target section)
    private static final int GHOST_SLOT_X = 14;
    private static final int GHOST_SLOT_Y = 78;

    // Section layout
    private static final int HEADER_Y = 5;
    private static final int HEADER_H = 14;
    private static final int STATUS_Y = 24;
    private static final int STATUS_H = 34;
    private static final int TARGET_Y = 62;
    private static final int TARGET_H = 76;
    private static final int CARD_OUTPUT_Y = 142;
    private static final int CARD_OUTPUT_H = 74;
    private static final int INV_Y = 220;

    private @Nullable ResourceLocation ghostTargetItemId;
    private ItemStack ghostTargetPreview = ItemStack.EMPTY;

    public MarketAutoTraderScreen(MarketAutoTraderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 230;
        this.imageHeight = 326;
    }

    @Override
    protected void init() {
        super.init();

        this.syncGhostTargetFromString(menu.targetItemId());

        int bx = leftPos;
        int by = topPos;

        this.addRenderableWidget(new ThemedButton(
                bx + 68, by + 98, 34, 16,
                Component.literal("Cap -"),
                btn -> sendConfigUpdate(currentTargetIdString(), Math.max(1, menu.priceCap() - 1), menu.batchSize())
        ));
        this.addRenderableWidget(new ThemedButton(
                bx + 104, by + 98, 34, 16,
                Component.literal("Cap +"),
                btn -> sendConfigUpdate(currentTargetIdString(), menu.priceCap() + 1, menu.batchSize())
        ));

        this.addRenderableWidget(new ThemedButton(
                bx + 12, by + 118, 50, 16,
                Component.literal("Clear"),
                btn -> {
                    clearGhostTarget();
                    sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize());
                }
        ));
        this.addRenderableWidget(new ThemedButton(
                bx + 68, by + 118, 34, 16,
                Component.literal("Stk -"),
                btn -> sendConfigUpdate(currentTargetIdString(), menu.priceCap(), Math.max(1, menu.batchSize() - 1))
        ));
        this.addRenderableWidget(new ThemedButton(
                bx + 104, by + 118, 34, 16,
                Component.literal("Stk +"),
                btn -> sendConfigUpdate(currentTargetIdString(), menu.priceCap(), Math.min(64, menu.batchSize() + 1))
        ));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        String menuTargetId = menu.targetItemId();
        if (ghostTargetItemId == null && menuTargetId != null && !menuTargetId.isBlank()) {
            syncGhostTargetFromString(menuTargetId);
        }
    }

    private String currentTargetIdString() {
        return ghostTargetItemId == null ? "" : ghostTargetItemId.toString();
    }

    private void sendConfigUpdate(String targetItemId, int priceCap, int batchSize) {
        MarketNetworking.sendAutoTraderConfig(
                menu.positionAccessor().asLong(),
                targetItemId,
                priceCap,
                batchSize
        );
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        ThemedUi ui = themed(guiGraphics);

        // Window
        ui.drawWindow(x, y, imageWidth, imageHeight);

        // Header
        drawPanel(guiGraphics, x + 5, y + HEADER_Y, imageWidth - 10, HEADER_H,
                UIScreenTheme.Machine.HEADER_FILL, UIScreenTheme.Machine.HEADER_BORDER);

        // Status section
        drawPanel(guiGraphics, x + 8, y + STATUS_Y, imageWidth - 16, STATUS_H,
                UIScreenTheme.Machine.SECTION_FILL, UIScreenTheme.Machine.SECTION_BORDER);

        // Progress bar (below status)
        int progressX = x + 12;
        int progressY = y + STATUS_Y + STATUS_H - 10;
        int progressWidth = imageWidth - 24;
        drawPanel(guiGraphics, progressX - 1, progressY - 1, progressWidth + 2, 8,
                UIScreenTheme.Machine.PROGRESS_FRAME_FILL, UIScreenTheme.Machine.PROGRESS_FRAME_BORDER);
        guiGraphics.fill(progressX, progressY, progressX + progressWidth, progressY + 6,
                UIScreenTheme.Machine.PROGRESS_TRACK_FILL);
        guiGraphics.fill(progressX, progressY, progressX + menu.progressScaled(progressWidth), progressY + 6,
                UIScreenTheme.Machine.PROGRESS_FILL_PRIMARY);

        // Target section
        drawPanel(guiGraphics, x + 8, y + TARGET_Y, imageWidth - 16, TARGET_H,
                UIScreenTheme.Machine.SECTION_FILL, UIScreenTheme.Machine.SECTION_BORDER);

        // Ghost slot for target item
        drawSlotFrame(guiGraphics, x + GHOST_SLOT_X, y + GHOST_SLOT_Y);
        if (!ghostTargetPreview.isEmpty()) {
            guiGraphics.renderItem(ghostTargetPreview, x + GHOST_SLOT_X, y + GHOST_SLOT_Y);
        }
        if (isMouseOverGhostSlot(mouseX, mouseY)) {
            guiGraphics.fill(x + GHOST_SLOT_X, y + GHOST_SLOT_Y, x + GHOST_SLOT_X + 16, y + GHOST_SLOT_Y + 16,
                    UIScreenTheme.Machine.GHOST_SLOT_OVERLAY);
        }

        // Card + Output section
        drawPanel(guiGraphics, x + 8, y + CARD_OUTPUT_Y, imageWidth - 16, CARD_OUTPUT_H,
                UIScreenTheme.Machine.SECTION_FILL, UIScreenTheme.Machine.SECTION_BORDER);

        // Card slot frame
        drawSlotFrame(guiGraphics, x + MarketAutoTraderMenu.CARD_X, y + MarketAutoTraderMenu.CARD_Y);

        // Output slot frames (3 rows x 9 cols)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(guiGraphics,
                        x + MarketAutoTraderMenu.OUTPUT_X + col * 18,
                        y + MarketAutoTraderMenu.OUTPUT_Y + row * 18);
            }
        }

        // Inventory section
        drawPanel(guiGraphics, x + 8, y + INV_Y, imageWidth - 16, imageHeight - INV_Y - 8,
                UIScreenTheme.Machine.SECTION_FILL, UIScreenTheme.Machine.SECTION_BORDER);

        // Player inventory slot frames
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotFrame(guiGraphics,
                        x + MarketAutoTraderMenu.PLAYER_INVENTORY_X + col * 18,
                        y + MarketAutoTraderMenu.PLAYER_INVENTORY_Y + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotFrame(guiGraphics,
                    x + MarketAutoTraderMenu.PLAYER_INVENTORY_X + col * 18,
                    y + MarketAutoTraderMenu.HOTBAR_Y);
        }

        // Status indicator light
        int statusColor = switch (menu.status()) {
            case MarketAutoTraderBlockEntity.STATUS_READY -> UIScreenTheme.Machine.STATUS_READY_TEXT;
            case MarketAutoTraderBlockEntity.STATUS_DISABLED -> UIScreenTheme.Machine.STATUS_DISABLED_TEXT;
            case MarketAutoTraderBlockEntity.STATUS_PRICE_TOO_HIGH, MarketAutoTraderBlockEntity.STATUS_NO_FUNDS,
                 MarketAutoTraderBlockEntity.STATUS_NO_STRESS, MarketAutoTraderBlockEntity.STATUS_NO_POWER -> UIScreenTheme.Machine.STATUS_ERROR_TEXT;
            case MarketAutoTraderBlockEntity.STATUS_NO_RPM -> UIScreenTheme.Machine.STATUS_WARNING_TEXT;
            default -> UIScreenTheme.Machine.STATUS_WARNING_TEXT;
        };
        guiGraphics.fill(x + imageWidth - 24, y + STATUS_Y + 8, x + imageWidth - 16, y + STATUS_Y + 16, statusColor);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Title
        guiGraphics.drawString(this.font, this.title, 11, HEADER_Y + 3, UIScreenTheme.Machine.TITLE_TEXT, false);

        // Status section
        guiGraphics.drawString(this.font, Component.literal("Status"), 12, STATUS_Y + 5, TEXT_COLOR, false);

        Component status = switch (menu.status()) {
            case MarketAutoTraderBlockEntity.STATUS_DISABLED -> Component.translatable("screen.incore.market.autotrader.status.disabled");
            case MarketAutoTraderBlockEntity.STATUS_NO_CARD -> Component.translatable("screen.incore.market.autotrader.status.no_card");
            case MarketAutoTraderBlockEntity.STATUS_NO_TARGET -> Component.translatable("screen.incore.market.autotrader.status.no_target");
            case MarketAutoTraderBlockEntity.STATUS_PRICE_TOO_HIGH -> Component.translatable("screen.incore.market.autotrader.status.price_too_high");
            case MarketAutoTraderBlockEntity.STATUS_NO_FUNDS -> Component.translatable("screen.incore.market.autotrader.status.no_funds");
            case MarketAutoTraderBlockEntity.STATUS_OUTPUT_FULL -> Component.translatable("screen.incore.market.autotrader.status.output_full");
            case MarketAutoTraderBlockEntity.STATUS_NO_RPM -> Component.translatable("screen.incore.market.autotrader.status.no_rpm");
            case MarketAutoTraderBlockEntity.STATUS_NO_STRESS -> Component.translatable("screen.incore.market.autotrader.status.no_stress");
            case MarketAutoTraderBlockEntity.STATUS_NO_POWER -> Component.translatable("screen.incore.market.autotrader.status.no_power");
            default -> Component.translatable("screen.incore.market.autotrader.status.ready");
        };
        List<FormattedCharSequence> statusLines = font.split(status, imageWidth - 72);
        for (int i = 0; i < statusLines.size() && i < 2; i++) {
            guiGraphics.drawString(this.font, statusLines.get(i), 56, STATUS_Y + 5 + i * this.font.lineHeight, TEXT_COLOR);
        }

        // Target section
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.market.autotrader.target"),
                12, TARGET_Y + 4, ACCENT_TEXT, false);
        guiGraphics.drawString(this.font, Component.translatable("screen.incore.market.autotrader.target_hint"),
                36, GHOST_SLOT_Y + 3, THEME.theme().text().muted(), false);

        // Cap and Stacks values - right-aligned next to the buttons
        guiGraphics.drawString(this.font, Component.literal("Cap: " + menu.priceCap()),
                144, 102, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal("Stacks: " + menu.batchSize()),
                144, 122, TEXT_COLOR, false);

        // Card + Output labels
        guiGraphics.drawString(this.font, Component.literal("Card"),
                MarketAutoTraderMenu.CARD_X - 4, MarketAutoTraderMenu.CARD_Y - 12, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal("Output"),
                MarketAutoTraderMenu.OUTPUT_X, CARD_OUTPUT_Y + 4, TEXT_COLOR, false);

        // Inventory label
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 12, INV_Y + 4, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
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
                sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize());
                return true;
            }

            if (button == 0) {
                ItemStack carried = menu.getCarried();
                if (!carried.isEmpty() && setGhostTargetFromItemStack(carried)) {
                    sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize());
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
        sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize());
        return true;
    }

    public void clearGhostTargetFromExternal() {
        clearGhostTarget();
        sendConfigUpdate(currentTargetIdString(), menu.priceCap(), menu.batchSize());
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
        ThemedUi ui = themed(guiGraphics);
        ui.drawRect(x, y, x + width, y + height, fillColor);
        ui.drawBorder(x, y, x + width, y + height, borderColor);
    }

    private static void drawSlotFrame(GuiGraphics guiGraphics, int x, int y) {
        themed(guiGraphics).drawSlotFrame(x, y);
    }

    private static ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, THEME.theme());
    }
}
