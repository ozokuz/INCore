package ozokuz.incore.client.features.gacha;

import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.client.ui.render.ThemedUi;
import ozokuz.incore.features.gacha.GachaRarity;
import ozokuz.incore.features.gacha.GachaService;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GachaBannerInfoScreen extends Screen {
    private static final UIScreenTheme THEME = UIScreenTheme.OTHER_CONTENT;
    private static final int REWARDS_PER_PAGE = 10;

    private final Screen parent;
    private final GachaService.BannerView banner;
    private int page;

    public GachaBannerInfoScreen(Screen parent, GachaService.BannerView banner) {
        super(Component.translatable("screen.incore.gacha_info.title", banner.name()));
        this.parent = parent;
        this.banner = banner;
        this.page = 0;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        List<GachaService.RewardView> rewards = banner.rewards();
        int totalPages = Math.max(1, (rewards.size() + REWARDS_PER_PAGE - 1) / REWARDS_PER_PAGE);
        page = Math.clamp(page, 0, totalPages - 1);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 40, this.height - 28, 80, 20)
                .build());

        if (totalPages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
                        page = Math.max(0, page - 1);
                        init();
                    })
                    .bounds(this.width / 2 - 120, this.height - 28, 20, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
                        page = Math.min(totalPages - 1, page + 1);
                        init();
                    })
                    .bounds(this.width / 2 + 100, this.height - 28, 20, 20)
                    .build());
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        themed(guiGraphics).drawBackdrop(this.width, this.height);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 14, UIScreenTheme.OtherContent.GACHA_TITLE_TEXT);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.pity", banner.pityFive(), 40, banner.pitySix(), 80),
                this.width / 2,
                28,
                UIScreenTheme.OtherContent.CATALOG_TEXT_META
        );
        if ("event".equals(banner.type())) {
            Component featuredLine = banner.eventFeaturedPityEnabled()
                    ? Component.translatable(
                            "screen.incore.gacha_banners.event_featured_pity",
                            banner.eventFeaturedPity(),
                            GachaService.EVENT_FEATURED_SIX_PITY_THRESHOLD
                    )
                    : Component.translatable("screen.incore.gacha_banners.event_featured_pity.unavailable");
            guiGraphics.drawCenteredString(this.font, featuredLine, this.width / 2, 40, UIScreenTheme.OtherContent.INFO_FEATURED_TEXT);
        } else {
            guiGraphics.drawCenteredString(
                    this.font,
                    Component.translatable(
                            "screen.incore.gacha_banners.basic_guaranteed_pity",
                            banner.basicSelectedSixPity(),
                            GachaService.BASIC_SELECTED_SIX_THRESHOLD
                    ),
                    this.width / 2,
                    40,
                    UIScreenTheme.OtherContent.INFO_RATE_LABEL_TEXT
            );
        }

        List<GachaService.RewardView> rewards = banner.rewards();
        int totalPages = Math.max(1, (rewards.size() + REWARDS_PER_PAGE - 1) / REWARDS_PER_PAGE);
        guiGraphics.drawCenteredString(
                this.font,
                Component.translatable("screen.incore.gacha_banners.page", page + 1, totalPages),
                this.width / 2,
                52,
                UIScreenTheme.OtherContent.INFO_NOTE_TEXT
        );

        int start = page * REWARDS_PER_PAGE;
        int end = Math.min(rewards.size(), start + REWARDS_PER_PAGE);
        int rowY = 68;
        int rowHeight = 16;
        int left = this.width / 2 - 140;
        int right = this.width / 2 + 140;
        List<Component> hoveredTooltip = null;

        for (int i = start; i < end; i++) {
            GachaService.RewardView reward = rewards.get(i);
            int row = i - start;
            int y = rowY + row * rowHeight;
            guiGraphics.fill(left, y - 1, right, y + 13, row % 2 == 0 ? UIScreenTheme.OtherContent.INFO_ROW_FILL_A : UIScreenTheme.OtherContent.INFO_ROW_FILL_B);

            ResourceLocation itemId = ResourceLocation.tryParse(reward.itemId());
            Item item = itemId == null ? Items.AIR : BuiltInRegistries.ITEM.get(itemId);
            ItemStack displayStack = item == Items.AIR ? ItemStack.EMPTY : item.getDefaultInstance();
            if (item != Items.AIR) {
                guiGraphics.renderItem(displayStack, left + 2, y - 2);
                Component itemName = item.getName(displayStack);
                guiGraphics.drawString(this.font, itemName, left + 22, y + 2, UIScreenTheme.OtherContent.INFO_ITEM_TEXT);
            } else {
                guiGraphics.drawString(this.font, reward.itemId(), left + 22, y + 2, UIScreenTheme.OtherContent.INFO_ITEM_MISSING_TEXT);
            }

            GachaRarity rarity = GachaRarity.fromStars(reward.rarity());
            guiGraphics.drawString(this.font, Component.literal(reward.rarity() + "★"), right - 72, y + 2, rarity.rgb());
            guiGraphics.drawString(
                    this.font,
                    Component.literal(String.format(Locale.ROOT, "%.2f%%", reward.chancePercent())),
                    right - 36,
                    y + 2,
                    UIScreenTheme.OtherContent.INFO_CHANCE_TEXT
            );

            if (mouseX >= left && mouseX < right && mouseY >= y - 1 && mouseY < y + 13) {
                List<Component> tooltip = new ArrayList<>();
                if (!displayStack.isEmpty()) {
                    tooltip.addAll(Screen.getTooltipFromItem(this.minecraft, displayStack));
                } else {
                    tooltip.add(Component.literal(reward.itemId()));
                }
                tooltip.add(Component.literal(reward.rarity() + "★").withColor(rarity.rgb()));
                tooltip.add(Component.literal(String.format(Locale.ROOT, "%.2f%%", reward.chancePercent())).withColor(UIScreenTheme.OtherContent.INFO_TOOLTIP_TEXT));
                hoveredTooltip = tooltip;
            }
        }

        if (hoveredTooltip != null) {
            guiGraphics.renderComponentTooltip(this.font, hoveredTooltip, mouseX, mouseY);
        }
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
