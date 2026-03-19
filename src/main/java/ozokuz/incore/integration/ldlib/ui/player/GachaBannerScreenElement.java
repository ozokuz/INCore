package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.FlexWrap;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;
import ozokuz.incore.features.gacha.network.GachaNetworking;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.INCoreUiIds;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;
import ozokuz.incore.integration.ldlib.ui.RequestPushIncoreUiPayload;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

final class GachaBannerScreenElement extends UIElement implements IBindable<String> {
    private String currentJson = "";
    private GachaService.ScreenData data = new GachaService.ScreenData("", List.of());
    private long syncedAtMs = System.currentTimeMillis();

    GachaBannerScreenElement() {
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
    public GachaBannerScreenElement setValue(@Nullable String value) {
        String nextJson = value == null ? "" : value;
        GachaService.ScreenData nextData = GachaAppUiSupport.parseScreenData(nextJson);
        currentJson = nextJson;
        syncedAtMs = System.currentTimeMillis();
        if (GachaAppUiSupport.isUiEquivalent(data, nextData) && !getChildren().isEmpty()) {
            data = nextData;
            return this;
        }
        data = nextData;
        rebuild();
        return this;
    }

    private void rebuild() {
        clearAllChildren();
        addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                            layout.flexDirection(FlexDirection.COLUMN);
                            layout.gapAll(8);
                        })
                        .addChildren(
                                GachaViewSupport.titleLabel(Component.translatable("screen.incore.gacha_banners.title")),
                                contentRow(),
                                footerRow()
                        )
        );
    }

    private UIElement contentRow() {
        return new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.widthPercent(100);
                    layout.minHeight(0);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(8);
                })
                .addChildren(sidebar(), detailsPanel());
    }

    private UIElement sidebar() {
        ScrollerView scroller = GachaViewSupport.scroller();
        for (GachaService.BannerView banner : data.banners()) {
            scroller.addScrollViewChild(bannerButton(banner));
        }

        return new UIElement()
                .layout(layout -> {
                    layout.width(GachaAppUiSupport.SIDEBAR_WIDTH);
                    layout.heightPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(4);
                })
                .addChildren(
                        GachaViewSupport.lineLabel(Component.translatable("screen.incore.gacha_banners.sidebar"), UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT),
                        GachaViewSupport.panel(scroller)
                );
    }

    private Button bannerButton(GachaService.BannerView banner) {
        boolean selected = banner.id().equals(data.selectedBannerId());
        Button button = GachaViewSupport.rowButton(Component.empty(), GachaAppUiSupport.BANNER_ROW_HEIGHT, selected
                ? UIScreenTheme.OtherContent.CATALOG_ROW_SELECTED_FILL
                : UIScreenTheme.OtherContent.CATALOG_ROW_FILL);
        int borderColor = selected
                ? GachaAppUiSupport.brightenColor(banner.sidebarColor(), 0.22F)
                : banner.sidebarColor();
        int hoverBorderColor = GachaAppUiSupport.brightenColor(borderColor, 0.16F);
        int fillColor = selected
                ? UIScreenTheme.OtherContent.CATALOG_ROW_SELECTED_FILL
                : UIScreenTheme.OtherContent.CATALOG_ROW_FILL;
        int hoverFillColor = selected
                ? UIScreenTheme.OtherContent.CATALOG_ROW_SELECTED_FILL
                : UIScreenTheme.OtherContent.CATALOG_ROW_FILL + 0x00080808;
        button.buttonStyle(style -> style
                .baseTexture(new BeveledRectTexture(fillColor, borderColor, borderColor, borderColor, 1, 0))
                .hoverTexture(new BeveledRectTexture(hoverFillColor, hoverBorderColor, hoverBorderColor, hoverBorderColor, 1, 0))
                .pressedTexture(new BeveledRectTexture(fillColor, hoverBorderColor, hoverBorderColor, hoverBorderColor, 1, 0))
        );
        button.text.setDisplay(false);

        ItemStack mainStack = GachaAppUiSupport.stackForId(banner.mainItemId());
        if (!mainStack.isEmpty()) {
            button.addChild(GachaViewSupport.icon(mainStack, 16));
        }

        UIElement textColumn = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(1);
        });

        Label nameLabel = GachaViewSupport.lineLabel(Component.literal(banner.name()), selected
                ? UIScreenTheme.OtherContent.GACHA_TEXT_SELECTED
                : UIScreenTheme.OtherContent.GACHA_TEXT_PRIMARY);
        nameLabel.textStyle(style -> style.textWrap(TextWrap.HIDE));

        Label secondaryLabel = banner.locked()
                ? GachaViewSupport.lineLabel(
                        Component.translatable("screen.incore.gacha_banners.locked", banner.requiredLevel()),
                        UIScreenTheme.OtherContent.GACHA_ERROR_TEXT
                )
                : GachaViewSupport.timerLabel(banner, () -> syncedAtMs, UIScreenTheme.OtherContent.GACHA_TEXT_SECONDARY);
        secondaryLabel.textStyle(style -> style.textWrap(TextWrap.HIDE));
        textColumn.addChildren(nameLabel, secondaryLabel);
        button.addChild(textColumn);

        button.setOnClick(event -> {
            if (banner.id().equals(data.selectedBannerId())) {
                return;
            }
            data = new GachaService.ScreenData(banner.id(), data.banners());
            rebuild();
            ResourceLocation bannerId = ResourceLocation.tryParse(banner.id());
            if (bannerId != null) {
                GachaNetworking.sendBannerSelection(bannerId);
            }
        });
        return button;
    }

    private UIElement detailsPanel() {
        GachaService.BannerView banner = selectedBanner();
        UIElement content = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.paddingAll(10);
            layout.gapAll(6);
        });

        if (banner == null) {
            content.addChildren(
                    GachaViewSupport.lineLabel(Component.translatable("incore.gacha.banner.none_configured"), UIScreenTheme.OtherContent.GACHA_ERROR_TEXT),
                    INCoreLdLibUiScaffold.spacer()
            );
            return GachaViewSupport.panel(content);
        }

        content.addChildren(
                GachaViewSupport.lineLabel(Component.literal(banner.name()), UIScreenTheme.OtherContent.GACHA_TEXT_PRIMARY),
                GachaViewSupport.lineLabel(
                        Component.translatable("screen.incore.gacha_banners.type." + banner.type()),
                        "basic".equals(banner.type())
                                ? UIScreenTheme.OtherContent.GACHA_BANNER_TYPE_BASIC_TEXT
                                : UIScreenTheme.OtherContent.GACHA_BANNER_TYPE_LIMITED_TEXT
                ),
                GachaViewSupport.lineLabel(
                        Component.translatable("screen.incore.gacha_banners.pity", banner.pityFive(), 40, banner.pitySix(), 80),
                        UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT
                ),
                pityDetailLabel(banner),
                GachaViewSupport.lineLabel(
                        Component.translatable("screen.incore.gacha_banners.time_left", GachaAppUiSupport.renderRemainingLabel(banner, syncedAtMs)),
                        UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT
                ).addEventListener(com.lowdragmc.lowdraglib2.gui.ui.event.UIEvents.TICK, event -> {
                    ((Label) event.currentElement).setText(Component.translatable(
                            "screen.incore.gacha_banners.time_left",
                            GachaAppUiSupport.renderRemainingLabel(banner, syncedAtMs)
                    ));
                }),
                showcaseArea(banner),
                INCoreLdLibUiScaffold.spacer()
        );

        return GachaViewSupport.panel(content);
    }

    private Label pityDetailLabel(GachaService.BannerView banner) {
        Component text = "event".equals(banner.type())
                ? (banner.eventFeaturedPityEnabled()
                        ? Component.translatable(
                                "screen.incore.gacha_banners.event_featured_pity",
                                banner.eventFeaturedPity(),
                                GachaService.EVENT_FEATURED_SIX_PITY_THRESHOLD
                        )
                        : Component.translatable("screen.incore.gacha_banners.event_featured_pity.unavailable"))
                : Component.translatable(
                        "screen.incore.gacha_banners.basic_guaranteed_pity",
                        banner.basicSelectedSixPity(),
                        GachaService.BASIC_SELECTED_SIX_THRESHOLD
                );
        return GachaViewSupport.lineLabel(text, "event".equals(banner.type())
                ? UIScreenTheme.OtherContent.GACHA_FEATURED_TEXT
                : UIScreenTheme.OtherContent.GACHA_DROP_RATE_TEXT);
    }

    private UIElement showcaseArea(GachaService.BannerView banner) {
        UIElement area = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        area.addChild(GachaViewSupport.centeredLabel(
                Component.translatable("screen.incore.gacha_banners.high_rarity_showcase"),
                UIScreenTheme.OtherContent.GACHA_PITY_LABEL_TEXT
        ));

        List<Item> sixStars = GachaAppUiSupport.uniqueRewardsByRarity(banner, 6);
        List<Item> fiveStars = GachaAppUiSupport.uniqueRewardsByRarity(banner, 5);
        if (sixStars.isEmpty() && fiveStars.isEmpty()) {
            area.addChild(GachaViewSupport.centeredLabel(
                    Component.translatable("screen.incore.gacha_banners.high_rarity_none"),
                    UIScreenTheme.OtherContent.GACHA_SHOWCASE_CHANCE_TEXT
            ));
            return area;
        }

        if (!sixStars.isEmpty()) {
            area.addChildren(
                    GachaViewSupport.centeredLabel(Component.translatable("screen.incore.gacha_banners.showcase.six"), UIScreenTheme.OtherContent.GACHA_SHOWCASE_SIX_TEXT),
                    iconGrid(sixStars, 5, 22)
            );
        }
        if (!fiveStars.isEmpty()) {
            area.addChildren(
                    GachaViewSupport.centeredLabel(Component.translatable("screen.incore.gacha_banners.showcase.five"), UIScreenTheme.OtherContent.GACHA_SHOWCASE_FIVE_TEXT),
                    iconGrid(fiveStars, 6, 18)
            );
        }
        return area;
    }

    private UIElement iconGrid(List<Item> items, int maxPerRow, int iconSize) {
        UIElement grid = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.flexWrap(FlexWrap.WRAP);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(6);
        });
        int count = 0;
        for (Item item : items) {
            if (count >= maxPerRow * 3) {
                break;
            }
            if (item != Items.AIR) {
                grid.addChild(GachaViewSupport.icon(item.getDefaultInstance(), iconSize));
                count++;
            }
        }
        return grid;
    }

    private UIElement footerRow() {
        GachaService.BannerView banner = selectedBanner();
        UIElement actionRow = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.FLEX_END);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(GachaAppUiSupport.FOOTER_GAP);
        });

        Button selectorButton = GachaViewSupport.footerButton(
                Component.translatable("screen.incore.gacha_banners.open_guaranteed_six_selector"),
                136,
                banner != null && banner.basicGuaranteeBlocked() && !banner.locked() && !banner.basicSelectableSixItems().isEmpty()
        );
        selectorButton.setOnClick(event -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new RequestPushIncoreUiPayload(INCoreUiIds.GACHA_GUARANTEED_SELECTION)
        ));

        Button infoButton = GachaViewSupport.footerButton(Component.translatable("screen.incore.gacha_banners.info"), 64, banner != null);
        infoButton.setOnClick(event -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(
                new RequestPushIncoreUiPayload(INCoreUiIds.GACHA_INFO)
        ));

        Button pullButton = GachaViewSupport.footerButton(
                Component.translatable("screen.incore.gacha_banners.pull_x10"),
                78,
                banner != null && !banner.locked() && !banner.basicGuaranteeBlocked()
        );
        pullButton.setOnClick(event -> {
            if (banner == null) {
                return;
            }
            ResourceLocation bannerId = ResourceLocation.tryParse(banner.id());
            if (bannerId != null) {
                GachaNetworking.sendBannerPurchase(bannerId);
            }
        });

        actionRow.addChildren(selectorButton, infoButton, pullButton);

        UIElement actionColumn = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.gapAll(0);
        });
        if (banner != null && !banner.permitUsage().isEmpty() && !banner.basicGuaranteeBlocked()) {
            actionColumn.addChild(permitUsagePanel(banner));
        }
        actionColumn.addChild(actionRow);

        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                    layout.alignItems(AlignItems.FLEX_END);
                })
                .addChildren(
                        GachaViewSupport.footerButton(Component.translatable("gui.done"), 80, true)
                                .setOnClick(event -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE)),
                        actionColumn
                );
    }

    private UIElement permitUsagePanel(GachaService.BannerView banner) {
        UIElement panel = new UIElement()
                .layout(layout -> {
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.FLEX_END);
                    layout.paddingTop(4);
                    layout.paddingRight(6);
                    layout.paddingBottom(4);
                    layout.paddingLeft(6);
                    layout.gapAll(2);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(UIScreenTheme.OtherContent.GACHA_BALANCE_PANEL_FILL)));
        for (GachaService.PermitUsageLineView line : banner.permitUsage()) {
            ItemStack stack = GachaAppUiSupport.stackForId(line.itemId());
            UIElement row = new UIElement().layout(layout -> {
                layout.flexDirection(FlexDirection.ROW);
                layout.alignItems(AlignItems.CENTER);
                layout.justifyContent(AlignContent.FLEX_END);
                layout.gapAll(4);
            });
            if (!stack.isEmpty()) {
                row.addChild(GachaViewSupport.icon(stack, 12));
            }
            row.addChild(GachaViewSupport.lineLabel(
                    Component.literal("x" + line.count()),
                    line.missing() ? UIScreenTheme.OtherContent.GACHA_COST_MISSING_TEXT : UIScreenTheme.OtherContent.GACHA_COST_OK_TEXT
            ));
            panel.addChild(row);
        }
        return panel;
    }

    private @Nullable GachaService.BannerView selectedBanner() {
        return GachaAppUiSupport.findBanner(data, null);
    }
}
