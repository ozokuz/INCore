package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;

final class GachaInfoScreenElement extends UIElement implements IBindable<String> {
    private String currentJson = "";
    private GachaService.ScreenData data = new GachaService.ScreenData("", List.of());
    private int page;

    GachaInfoScreenElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        internalSetup();
        rebuild();
    }

    @Override
    public String getValue() {
        return currentJson;
    }

    @Override
    public GachaInfoScreenElement setValue(@Nullable String value) {
        String nextJson = value == null ? "" : value;
        GachaService.ScreenData nextData = GachaAppUiSupport.parseScreenData(nextJson);
        String previousSelectedId = data.selectedBannerId();
        currentJson = nextJson;
        if (GachaAppUiSupport.isUiEquivalent(data, nextData) && !getChildren().isEmpty()) {
            data = nextData;
            return this;
        }
        data = nextData;
        if (!previousSelectedId.equals(data.selectedBannerId())) {
            page = 0;
        }
        rebuild();
        return this;
    }

    private void rebuild() {
        clearAllChildren();
        GachaService.BannerView banner = selectedBanner();
        int totalPages = banner == null ? 1 : GachaAppUiSupport.totalRewardPages(banner);
        page = Math.clamp(page, 0, totalPages - 1);

        ScrollerView scroller = GachaViewSupport.scroller();
        if (banner != null) {
            List<GachaService.RewardView> rewards = GachaAppUiSupport.rewardsForPage(banner, page);
            for (int index = 0; index < rewards.size(); index++) {
                scroller.addScrollViewChild(oddsRow(rewards.get(index), index));
            }
        }

        Button prevButton = GachaViewSupport.footerButton(Component.literal("<"), 20, page > 0);
        prevButton.setOnClick(event -> {
            page = Math.max(0, page - 1);
            rebuild();
        });

        Button nextButton = GachaViewSupport.footerButton(Component.literal(">"), 20, page < totalPages - 1);
        nextButton.setOnClick(event -> {
            page = Math.min(totalPages - 1, page + 1);
            rebuild();
        });

        addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                            layout.flexDirection(FlexDirection.COLUMN);
                            layout.alignItems(AlignItems.CENTER);
                            layout.gapAll(4);
                        })
                        .addChildren(
                                GachaViewSupport.titleLabel(Component.translatable("screen.incore.gacha_info.title", banner == null ? "" : banner.name())),
                                GachaViewSupport.centeredLabel(
                                        banner == null
                                                ? Component.empty()
                                                : Component.translatable("screen.incore.gacha_banners.pity", banner.pityFive(), 40, banner.pitySix(), 80),
                                        UIScreenTheme.OtherContent.CATALOG_TEXT_META
                                ),
                                GachaViewSupport.centeredLabel(
                                        featuredOrGuaranteedLine(banner),
                                        "event".equals(banner == null ? "" : banner.type())
                                                ? UIScreenTheme.OtherContent.INFO_FEATURED_TEXT
                                                : UIScreenTheme.OtherContent.INFO_RATE_LABEL_TEXT
                                ),
                                GachaViewSupport.centeredLabel(
                                        Component.translatable("screen.incore.gacha_banners.page", page + 1, totalPages),
                                        UIScreenTheme.OtherContent.GACHA_PAGE_TEXT
                                ),
                                new UIElement()
                                        .layout(layout -> {
                                            layout.flex(1);
                                            layout.width(340);
                                            layout.minHeight(0);
                                        })
                                        .addChild(GachaViewSupport.panel(scroller)),
                                new UIElement()
                                        .layout(layout -> {
                                            layout.widthPercent(100);
                                            layout.flexDirection(FlexDirection.ROW);
                                            layout.justifyContent(AlignContent.CENTER);
                                            layout.alignItems(AlignItems.CENTER);
                                            layout.gapAll(12);
                                        })
                                        .addChildren(
                                                prevButton,
                                                GachaViewSupport.footerButton(Component.translatable("gui.back"), 120, true)
                                                        .setOnClick(event -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE)),
                                                nextButton
                                        )
                        )
        );
    }

    private UIElement oddsRow(GachaService.RewardView reward, int index) {
        UIElement row = new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(18);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.CENTER);
                    layout.paddingHorizontal(4);
                    layout.gapAll(6);
                })
                .style(style -> style.backgroundTexture(RectTexture.of((index & 1) == 0
                        ? UIScreenTheme.OtherContent.INFO_ROW_FILL_A
                        : UIScreenTheme.OtherContent.INFO_ROW_FILL_B)));
        ItemStack stack = GachaAppUiSupport.stackForId(reward.itemId());
        row.style(style -> style.tooltips(
                GachaViewSupport.stackTooltip(
                        stack,
                        Component.literal(reward.rarity() + "★"),
                        Component.literal(String.format(Locale.ROOT, "%.2f%%", reward.chancePercent()))
                )
        ));
        if (!stack.isEmpty()) {
            row.addChild(GachaViewSupport.tooltipIcon(stack, 14, GachaViewSupport.stackTooltip(stack)));
        }
        var nameLabel = GachaViewSupport.lineLabel(
                stack.isEmpty() ? Component.literal(reward.itemId()) : stack.getHoverName(),
                stack.isEmpty() ? UIScreenTheme.OtherContent.INFO_ITEM_MISSING_TEXT : UIScreenTheme.OtherContent.INFO_ITEM_TEXT
        );
        nameLabel.layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
        });
        nameLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
        );
        row.addChild(nameLabel);

        var rarityLabel = GachaViewSupport.lineLabel(
                Component.literal(reward.rarity() + "★"),
                GachaAppUiSupport.rarityColor(reward.rarity())
        );
        rarityLabel.layout(layout -> layout.width(34));
        rarityLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.RIGHT)
        );
        row.addChild(rarityLabel);

        var chanceLabel = GachaViewSupport.lineLabel(
                Component.literal(String.format(java.util.Locale.ROOT, "%.2f%%", reward.chancePercent())),
                UIScreenTheme.OtherContent.INFO_CHANCE_TEXT
        );
        chanceLabel.layout(layout -> layout.width(48));
        chanceLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.RIGHT)
        );
        row.addChild(chanceLabel);
        return row;
    }

    private static Component featuredOrGuaranteedLine(@Nullable GachaService.BannerView banner) {
        if (banner == null) {
            return Component.empty();
        }
        if ("event".equals(banner.type())) {
            return banner.eventFeaturedPityEnabled()
                    ? Component.translatable(
                            "screen.incore.gacha_banners.event_featured_pity",
                            banner.eventFeaturedPity(),
                            GachaService.EVENT_FEATURED_SIX_PITY_THRESHOLD
                    )
                    : Component.translatable("screen.incore.gacha_banners.event_featured_pity.unavailable");
        }
        return Component.translatable(
                "screen.incore.gacha_banners.basic_guaranteed_pity",
                banner.basicSelectedSixPity(),
                GachaService.BASIC_SELECTED_SIX_THRESHOLD
        );
    }

    private @Nullable GachaService.BannerView selectedBanner() {
        return GachaAppUiSupport.findBanner(data, null);
    }
}
