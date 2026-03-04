package io.github.ozokuz.incore.client.features.gacha;

import io.github.ozokuz.incore.client.ui.UIScreenTheme;
import io.github.ozokuz.incore.client.ui.render.ThemedUi;
import io.github.ozokuz.incore.features.gacha.GachaService;
import io.github.ozokuz.incore.features.gacha.network.GachaNetworking;
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

import java.util.ArrayList;
import java.util.List;

public class GachaGuaranteedSixSelectionScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.OTHER_CONTENT;
    private static final int CARD_HEIGHT = 90;
    private static final int CARD_GAP = 8;

    private final Screen parent;
    private final GachaService.BannerView banner;
    private final List<ResourceLocation> selectableItems;
    private final List<CardLayout> cardLayouts = new ArrayList<>();
    private @Nullable Button confirmButton;
    private int selectedIndex = -1;

    public GachaGuaranteedSixSelectionScreen(Screen parent, GachaService.BannerView banner) {
        super(Component.translatable("screen.incore.gacha_guaranteed_six.title", banner.name()));
        this.parent = parent;
        this.banner = banner;
        this.selectableItems = collectSelectableItems(banner);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.cardLayouts.clear();

        int footerY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 130, footerY, 120, 20)
                .build());

        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("screen.incore.gacha_guaranteed_six.confirm"),
                        button -> confirmSelection()
                )
                .bounds(this.width / 2 + 10, footerY, 120, 20)
                .build());
        this.confirmButton.active = selectedIndex >= 0 && selectedIndex < selectableItems.size();

        layoutCards();
    }

    private void layoutCards() {
        if (selectableItems.isEmpty()) {
            return;
        }

        int availableWidth = this.width - 32;
        int maxColumns = Math.max(1, Math.min(selectableItems.size(), 6));
        int cardWidth = 96;
        int columns = maxColumns;
        while (columns > 1 && (columns * cardWidth + (columns - 1) * CARD_GAP) > availableWidth) {
            columns--;
        }
        int totalWidth = columns * cardWidth + (columns - 1) * CARD_GAP;
        int startX = (this.width - totalWidth) / 2;
        int startY = 68;

        for (int i = 0; i < selectableItems.size(); i++) {
            int row = i / columns;
            int col = i % columns;
            int left = startX + col * (cardWidth + CARD_GAP);
            int top = startY + row * (CARD_HEIGHT + CARD_GAP);
            cardLayouts.add(new CardLayout(i, left, top, left + cardWidth, top + CARD_HEIGHT));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, 0xF6F6F6);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable(
                        "screen.incore.gacha_banners.basic_guaranteed_pity",
                        banner.basicSelectedSixPity(),
                        GachaService.BASIC_SELECTED_SIX_THRESHOLD
                ),
                this.width / 2,
                28,
                0xC2E9FF
        );
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.gacha_guaranteed_six.select_hint"),
                this.width / 2,
                42,
                0xD9D9D9
        );

        if (selectableItems.isEmpty()) {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable("screen.incore.gacha_guaranteed_six.none_available"),
                    this.width / 2,
                    this.height / 2,
                    0xE87E7E
            );
            return;
        }

        @Nullable CardLayout hovered = null;
        for (CardLayout layout : cardLayouts) {
            boolean selected = layout.index() == selectedIndex;
            boolean isHovered = layout.contains(mouseX, mouseY);
            if (isHovered) {
                hovered = layout;
            }

            int border = selected ? 0xFF6BD5FF : (isHovered ? 0xFF8F8F8F : 0xFF4D4D4D);
            int fill = selected ? 0xA0223A4A : 0xA01D1D1D;
            guiGraphics.fill(layout.left(), layout.top(), layout.right(), layout.bottom(), fill);
            guiGraphics.fill(layout.left(), layout.top(), layout.right(), layout.top() + 1, border);
            guiGraphics.fill(layout.left(), layout.bottom() - 1, layout.right(), layout.bottom(), border);
            guiGraphics.fill(layout.left(), layout.top(), layout.left() + 1, layout.bottom(), border);
            guiGraphics.fill(layout.right() - 1, layout.top(), layout.right(), layout.bottom(), border);

            ResourceLocation itemId = selectableItems.get(layout.index());
            Item item = itemFromId(itemId);
            ItemStack stack = item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
            if (!stack.isEmpty()) {
                int iconX = layout.left() + (layout.right() - layout.left() - 16) / 2;
                int iconY = layout.top() + 10;
                guiGraphics.renderItem(stack, iconX, iconY);
            }

            Component name = item == Items.AIR ? Component.literal(itemId.toString()) : item.getName(item.getDefaultInstance());
            String clippedName = this.font.plainSubstrByWidth(name.getString(), layout.right() - layout.left() - 12);
            int nameX = layout.left() + (layout.right() - layout.left() - this.font.width(clippedName)) / 2;
            guiGraphics.drawString(this.font, clippedName, nameX, layout.top() + 34, 0xF0F0F0, false);

            Component rawId = Component.literal(itemId.toString());
            String clippedId = this.font.plainSubstrByWidth(rawId.getString(), layout.right() - layout.left() - 12);
            int idX = layout.left() + (layout.right() - layout.left() - this.font.width(clippedId)) / 2;
            guiGraphics.drawString(this.font, clippedId, idX, layout.top() + 50, 0xAFAFAF, false);

            guiGraphics.drawCenteredString(
                    this.font,
                    Component.literal("6★"),
                    (layout.left() + layout.right()) / 2,
                    layout.bottom() - 16,
                    0xFF8C8C
            );
        }

        if (hovered != null) {
            ResourceLocation itemId = selectableItems.get(hovered.index());
            Item item = itemFromId(itemId);
            ItemStack stack = item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
            List<Component> tooltip = new ArrayList<>();
            if (!stack.isEmpty()) {
                tooltip.addAll(Screen.getTooltipFromItem(this.minecraft, stack));
            } else {
                tooltip.add(Component.literal(itemId.toString()));
            }
            tooltip.add(Component.literal("6★").withColor(0xFF8C8C));
            guiGraphics.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (CardLayout layout : cardLayouts) {
                if (layout.contains(mouseX, mouseY)) {
                    selectedIndex = layout.index();
                    if (confirmButton != null) {
                        confirmButton.active = true;
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void confirmSelection() {
        if (selectedIndex < 0 || selectedIndex >= selectableItems.size()) {
            return;
        }

        ResourceLocation bannerId = ResourceLocation.tryParse(banner.id());
        if (bannerId == null) {
            return;
        }

        ResourceLocation itemId = selectableItems.get(selectedIndex);
        GachaNetworking.sendBasicGuaranteedSixClaim(bannerId, itemId);
        if (confirmButton != null) {
            confirmButton.active = false;
        }
    }

    private static List<ResourceLocation> collectSelectableItems(GachaService.BannerView banner) {
        List<ResourceLocation> result = new ArrayList<>();
        for (String rawId : banner.basicSelectableSixItems()) {
            ResourceLocation parsed = ResourceLocation.tryParse(rawId);
            if (parsed != null && !result.contains(parsed)) {
                result.add(parsed);
            }
        }
        return result;
    }

    private static Item itemFromId(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.get(itemId);
    }

    private record CardLayout(int index, int left, int top, int right, int bottom) {
        private boolean contains(double x, double y) {
            return x >= left && x < right && y >= top && y < bottom;
        }
    }

    private ThemedUi themed(GuiGraphics guiGraphics) {
        return new ThemedUi(guiGraphics, this.font, THEME.theme());
    }
}
