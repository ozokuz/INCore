package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.texture.RectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollDisplay;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyPosition;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.client.ui.UIScreenTheme;
import ozokuz.incore.features.gacha.GachaService;
import ozokuz.incore.features.gacha.network.GachaNetworking;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;

final class GachaAppUiElement extends UIElement implements IBindable<String> {
    private enum Mode {
        BANNERS,
        INFO,
        GUARANTEE_SELECT
    }

    private String currentJson = "";
    private GachaService.ScreenData data = new GachaService.ScreenData(List.of());
    private Mode mode = Mode.BANNERS;
    private @Nullable String selectedBannerId;
    private @Nullable ResourceLocation selectedGuaranteedItem;
    private int infoPage;
    private long syncedAtMs = System.currentTimeMillis();

    private final UIElement bannerModeView;
    private final UIElement infoModeView;
    private final UIElement guaranteeModeView;
    private final Button bannerSelectorButton;
    private final Button bannerInfoButton;
    private final Button bannerPullButton;
    private final Label infoTitleLabel;
    private final Label infoPityLabel;
    private final Label infoFeaturedLabel;
    private final Label infoPageLabel;
    private final Button infoPrevButton;
    private final Button infoNextButton;
    private final Label guaranteeTitleLabel;
    private final Label guaranteePityLabel;
    private final Button guaranteeConfirmButton;
    private @Nullable Mode displayedMode;
    private @Nullable Boolean bannerSelectorVisible;
    private @Nullable Boolean bannerInfoActive;
    private @Nullable Boolean bannerPullActive;
    private @Nullable Boolean infoPrevActive;
    private @Nullable Boolean infoNextActive;
    private @Nullable Boolean guaranteeConfirmActive;
    private @Nullable String infoTitleText;
    private @Nullable String infoPityText;
    private @Nullable String infoFeaturedText;
    private int infoFeaturedColor = Integer.MIN_VALUE;
    private @Nullable String infoPageText;
    private @Nullable String guaranteeTitleText;
    private @Nullable String guaranteePityText;

    GachaAppUiElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });

        bannerSelectorButton = button(
                Component.translatable("screen.incore.gacha_banners.open_guaranteed_six_selector"),
                136,
                true,
                () -> {
                    mode = Mode.GUARANTEE_SELECT;
                    selectedGuaranteedItem = null;
                    reconcileState();
                    refreshState();
                }
        );
        bannerInfoButton = button(
                Component.translatable("screen.incore.gacha_banners.info"),
                64,
                false,
                () -> {
                    mode = Mode.INFO;
                    infoPage = 0;
                    reconcileState();
                    refreshState();
                }
        );
        bannerPullButton = button(
                Component.translatable("screen.incore.gacha_banners.pull_x10"),
                78,
                false,
                () -> {
                    GachaService.BannerView banner = selectedBanner();
                    if (banner == null) {
                        return;
                    }
                    ResourceLocation bannerId = ResourceLocation.tryParse(banner.id());
                    if (bannerId != null) {
                        GachaNetworking.sendBannerPurchase(bannerId);
                    }
                }
        );
        infoTitleLabel = centeredTitle(Component.empty());
        infoPityLabel = centeredInfoLabel(Component.empty(), UIScreenTheme.OtherContent.CATALOG_TEXT_META);
        infoFeaturedLabel = centeredInfoLabel(Component.empty(), UIScreenTheme.OtherContent.INFO_RATE_LABEL_TEXT);
        infoPageLabel = centeredInfoLabel(Component.empty(), UIScreenTheme.OtherContent.INFO_NOTE_TEXT);
        infoPrevButton = button(Component.literal("<"), 20, false, () -> {
            infoPage = Math.max(0, infoPage - 1);
            refreshState();
        });
        infoNextButton = button(Component.literal(">"), 20, false, () -> {
            GachaService.BannerView banner = selectedBanner();
            int totalPages = banner == null ? 1 : GachaAppUiSupport.totalRewardPages(banner);
            infoPage = Math.min(totalPages - 1, infoPage + 1);
            refreshState();
        });
        guaranteeTitleLabel = centeredTitle(Component.empty());
        guaranteePityLabel = centeredInfoLabel(Component.empty(), UIScreenTheme.OtherContent.INFO_RATE_LABEL_TEXT);
        guaranteeConfirmButton = button(
                Component.translatable("screen.incore.gacha_guaranteed_six.confirm"),
                140,
                false,
                () -> {
                    GachaService.BannerView banner = selectedBanner();
                    if (banner == null || selectedGuaranteedItem == null) {
                        return;
                    }
                    ResourceLocation bannerId = ResourceLocation.tryParse(banner.id());
                    if (bannerId != null) {
                        GachaNetworking.sendBasicGuaranteedSixClaim(bannerId, selectedGuaranteedItem);
                    }
                }
        );

        bannerModeView = buildBannerMode();
        infoModeView = buildInfoMode();
        guaranteeModeView = buildGuaranteeMode();

        addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                            layout.paddingTop(12);
                            layout.paddingRight(16);
                            layout.paddingBottom(12);
                            layout.paddingLeft(16);
                        })
                        .addChildren(bannerModeView, infoModeView, guaranteeModeView)
        );

        refreshState();
    }

    @Override
    public String getValue() {
        return currentJson;
    }

    @Override
    public GachaAppUiElement setValue(@Nullable String value) {
        String next = value == null ? "" : value;
        GachaService.ScreenData nextData = GachaAppUiSupport.parseScreenData(next);
        currentJson = next;
        syncedAtMs = System.currentTimeMillis();
        if (GachaAppUiSupport.isUiEquivalent(data, nextData)) {
            data = nextData;
            return this;
        }
        data = nextData;
        reconcileState();
        refreshState();
        return this;
    }

    List<GachaService.BannerView> banners() {
        return data.banners();
    }

    @Nullable String selectedBannerId() {
        return selectedBannerId;
    }

    long syncedAtMs() {
        return syncedAtMs;
    }

    int infoPage() {
        return infoPage;
    }

    @Nullable ResourceLocation selectedGuaranteedItem() {
        return selectedGuaranteedItem;
    }

    @Nullable GachaService.BannerView selectedBanner() {
        return GachaAppUiSupport.findBanner(data, selectedBannerId);
    }

    List<ResourceLocation> selectableSixItems() {
        GachaService.BannerView banner = selectedBanner();
        return banner == null ? List.of() : GachaAppUiSupport.selectableSixItems(banner);
    }

    void selectBanner(String bannerId) {
        if (bannerId.equals(selectedBannerId)) {
            return;
        }
        selectedBannerId = bannerId;
        infoPage = 0;
        selectedGuaranteedItem = null;
        reconcileState();
        refreshState();
    }

    void selectGuaranteedItem(ResourceLocation itemId) {
        if (itemId.equals(selectedGuaranteedItem)) {
            return;
        }
        selectedGuaranteedItem = itemId;
        refreshState();
    }

    private void reconcileState() {
        GachaService.BannerView banner = GachaAppUiSupport.findBanner(data, selectedBannerId);
        selectedBannerId = banner == null ? null : banner.id();
        if (mode == Mode.INFO && banner == null) {
            mode = Mode.BANNERS;
        }
        if (mode == Mode.GUARANTEE_SELECT && (banner == null || !banner.basicGuaranteeBlocked())) {
            mode = Mode.BANNERS;
            selectedGuaranteedItem = null;
        }
        if (banner != null) {
            infoPage = Math.clamp(infoPage, 0, GachaAppUiSupport.totalRewardPages(banner) - 1);
            if (selectedGuaranteedItem != null && !selectableSixItems().contains(selectedGuaranteedItem)) {
                selectedGuaranteedItem = null;
            }
        } else {
            infoPage = 0;
            selectedGuaranteedItem = null;
        }
    }

    private void refreshState() {
        GachaService.BannerView banner = selectedBanner();

        displayedMode = setModeDisplay(displayedMode, mode);

        bannerSelectorVisible = setDisplayIfChanged(
                bannerSelectorButton,
                bannerSelectorVisible,
                banner != null && banner.basicGuaranteeBlocked() && !banner.locked() && !selectableSixItems().isEmpty()
        );
        bannerInfoActive = setButtonActiveIfChanged(bannerInfoButton, bannerInfoActive, banner != null);
        bannerPullActive = setButtonActiveIfChanged(bannerPullButton, bannerPullActive, banner != null && !banner.locked() && !banner.basicGuaranteeBlocked());

        infoTitleText = setLabelTextIfChanged(
                infoTitleLabel,
                infoTitleText,
                Component.translatable("screen.incore.gacha_info.title", banner == null ? "" : banner.name())
        );
        infoPityText = setLabelTextIfChanged(
                infoPityLabel,
                infoPityText,
                banner == null
                        ? Component.empty()
                        : Component.translatable("screen.incore.gacha_banners.pity", banner.pityFive(), 40, banner.pitySix(), 80)
        );
        infoFeaturedText = setLabelTextIfChanged(infoFeaturedLabel, infoFeaturedText, featuredOrGuaranteedLine(banner));
        infoFeaturedColor = setLabelColorIfChanged(
                infoFeaturedLabel,
                infoFeaturedColor,
                "event".equals(banner == null ? "" : banner.type())
                        ? UIScreenTheme.OtherContent.INFO_FEATURED_TEXT
                        : UIScreenTheme.OtherContent.INFO_RATE_LABEL_TEXT
        );
        int totalPages = banner == null ? 1 : GachaAppUiSupport.totalRewardPages(banner);
        infoPageText = setLabelTextIfChanged(
                infoPageLabel,
                infoPageText,
                Component.translatable("screen.incore.gacha_banners.page", infoPage + 1, totalPages)
        );
        infoPrevActive = setButtonActiveIfChanged(infoPrevButton, infoPrevActive, infoPage > 0);
        infoNextActive = setButtonActiveIfChanged(infoNextButton, infoNextActive, infoPage < totalPages - 1);

        guaranteeTitleText = setLabelTextIfChanged(
                guaranteeTitleLabel,
                guaranteeTitleText,
                Component.translatable("screen.incore.gacha_guaranteed_six.title", banner == null ? "" : banner.name())
        );
        guaranteePityText = setLabelTextIfChanged(
                guaranteePityLabel,
                guaranteePityText,
                banner == null
                        ? Component.empty()
                        : Component.translatable(
                                "screen.incore.gacha_banners.basic_guaranteed_pity",
                                banner.basicSelectedSixPity(),
                                GachaService.BASIC_SELECTED_SIX_THRESHOLD
                        )
        );
        guaranteeConfirmActive = setButtonActiveIfChanged(
                guaranteeConfirmButton,
                guaranteeConfirmActive,
                banner != null && selectedGuaranteedItem != null
        );
    }

    private UIElement buildBannerMode() {
        return modeLayer()
                .addChildren(
                        centeredTitle(Component.translatable("screen.incore.gacha_banners.title")),
                        new UIElement()
                                .layout(layout -> {
                                    layout.flex(1);
                                    layout.widthPercent(100);
                                    layout.minHeight(0);
                                    layout.flexDirection(FlexDirection.ROW);
                                    layout.gapAll(8);
                                })
                                .addChildren(buildSidebar(), buildDetailPanel()),
                        buildBannerFooter()
                );
    }

    private UIElement buildSidebar() {
        GachaBannerRailElement rail = new GachaBannerRailElement(this);
        ScrollerView scroller = new ScrollerView()
                .scrollerStyle(style -> style
                        .mode(ScrollerMode.VERTICAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                );
        scroller.layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
        });
        scroller.viewPort.style(style -> style.backgroundTexture(RectTexture.of(0x00000000)));
        scroller.viewContainer.layout(layout -> layout.widthPercent(100));
        scroller.verticalScroller.layout(layout -> layout.width(6));
        scroller.verticalScroller.headButton.setDisplay(false);
        scroller.verticalScroller.tailButton.setDisplay(false);
        scroller.horizontalScroller.headButton.setDisplay(false);
        scroller.horizontalScroller.tailButton.setDisplay(false);
        scroller.verticalScroller.scrollContainer.style(style -> style.backgroundTexture(TaskOverviewUiSupport.SCROLL_TRACK_TEXTURE));
        scroller.verticalScroller.scrollBar.buttonStyle(style -> style
                .baseTexture(TaskOverviewUiSupport.SCROLL_THUMB_IDLE_TEXTURE)
                .hoverTexture(TaskOverviewUiSupport.SCROLL_THUMB_HOVER_TEXTURE)
                .pressedTexture(TaskOverviewUiSupport.SCROLL_THUMB_PRESSED_TEXTURE)
        );
        scroller.addScrollViewChild(rail);

        return new UIElement()
                .layout(layout -> {
                    layout.width(GachaAppUiSupport.SIDEBAR_WIDTH);
                    layout.heightPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(4);
                })
                .addChildren(
                        new UIElement()
                                .layout(layout -> {
                                    layout.flex(1);
                                    layout.minHeight(0);
                                })
                                .style(style -> style.backgroundTexture(RectTexture.of(GachaAppUiSupport.BANNER_PANEL_FILL)))
                                .addChild(scroller)
                );
    }

    private UIElement buildDetailPanel() {
        return new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.heightPercent(100);
                })
                .style(style -> style.backgroundTexture(RectTexture.of(GachaAppUiSupport.BANNER_PANEL_FILL)))
                .addChild(
                        new GachaBannerDetailsElement(this).layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                        })
                );
    }

    private UIElement buildBannerFooter() {
        UIElement buttonRow = new UIElement().layout(layout -> {
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(GachaAppUiSupport.FOOTER_GAP);
            layout.alignItems(AlignItems.CENTER);
        });
        buttonRow.addChildren(bannerSelectorButton, bannerInfoButton, bannerPullButton);

        return new UIElement()
                .layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                    layout.alignItems(AlignItems.CENTER);
                })
                .addChildren(
                        button(Component.translatable("gui.done"), 80, true, () -> PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE)),
                        buttonRow
                );
    }

    private UIElement buildInfoMode() {
        return modeLayer()
                .addChildren(
                        infoTitleLabel,
                        infoPityLabel,
                        infoFeaturedLabel,
                        infoPageLabel,
                        new UIElement()
                                .layout(layout -> {
                                    layout.flex(1);
                                    layout.width(280);
                                    layout.minHeight(0);
                                })
                                .addChild(
                                        new GachaOddsTableElement(this).layout(layout -> {
                                            layout.widthPercent(100);
                                            layout.heightPercent(100);
                                        })
                                ),
                        new UIElement()
                                .layout(layout -> {
                                    layout.widthPercent(100);
                                    layout.flexDirection(FlexDirection.ROW);
                                    layout.justifyContent(AlignContent.CENTER);
                                    layout.alignItems(AlignItems.CENTER);
                                    layout.gapAll(12);
                                })
                                .addChildren(
                                        infoPrevButton,
                                        button(Component.translatable("gui.back"), 120, true, () -> {
                                            mode = Mode.BANNERS;
                                            refreshState();
                                        }),
                                        infoNextButton
                                )
                );
    }

    private UIElement buildGuaranteeMode() {
        return modeLayer()
                .addChildren(
                        guaranteeTitleLabel,
                        guaranteePityLabel,
                        centeredInfoLabel(
                                Component.translatable("screen.incore.gacha_guaranteed_six.select_hint"),
                                UIScreenTheme.OtherContent.CATALOG_TEXT_META
                        ),
                        new GachaGuaranteedSelectionElement(this).layout(layout -> {
                            layout.flex(1);
                            layout.widthPercent(100);
                            layout.minHeight(0);
                        }),
                        new UIElement()
                                .layout(layout -> {
                                    layout.widthPercent(100);
                                    layout.flexDirection(FlexDirection.ROW);
                                    layout.justifyContent(AlignContent.CENTER);
                                    layout.alignItems(AlignItems.CENTER);
                                    layout.gapAll(16);
                                })
                                .addChildren(
                                        button(Component.translatable("gui.back"), 120, true, () -> {
                                            mode = Mode.BANNERS;
                                            refreshState();
                                        }),
                                        guaranteeConfirmButton
                                )
                );
    }

    private Component featuredOrGuaranteedLine(@Nullable GachaService.BannerView banner) {
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

    private static UIElement modeLayer() {
        return new UIElement()
                .layout(layout -> {
                    layout.positionType(TaffyPosition.ABSOLUTE);
                    layout.left(0);
                    layout.top(0);
                    layout.widthPercent(100);
                    layout.heightPercent(100);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.alignItems(AlignItems.CENTER);
                    layout.gapAll(4);
                });
    }

    private Mode setModeDisplay(@Nullable Mode previousMode, Mode nextMode) {
        if (previousMode == nextMode) {
            return nextMode;
        }
        bannerModeView.setDisplay(nextMode == Mode.BANNERS);
        infoModeView.setDisplay(nextMode == Mode.INFO);
        guaranteeModeView.setDisplay(nextMode == Mode.GUARANTEE_SELECT);
        return nextMode;
    }

    private static Boolean setDisplayIfChanged(UIElement element, @Nullable Boolean previous, boolean visible) {
        if (previous == null || previous != visible) {
            element.setDisplay(visible);
        }
        return visible;
    }

    private static Boolean setButtonActiveIfChanged(Button button, @Nullable Boolean previous, boolean active) {
        if (previous == null || previous != active) {
            setButtonActive(button, active);
        }
        return active;
    }

    private static @Nullable String setLabelTextIfChanged(Label label, @Nullable String previous, Component text) {
        String next = text.getString();
        if (previous == null || !previous.equals(next)) {
            label.setText(text);
        }
        return next;
    }

    private static int setLabelColorIfChanged(Label label, int previous, int color) {
        if (previous == color) {
            return color;
        }
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
                .textColor(color)
        );
        return color;
    }

    private static void setButtonActive(Button button, boolean active) {
        button.setActive(active);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textColor(active ? TaskOverviewUiSupport.TEXT_PRIMARY : TaskOverviewUiSupport.TEXT_MUTED)
        );
        button.buttonStyle(style -> style
                .baseTexture(active ? TaskOverviewUiSupport.BUTTON_IDLE_TEXTURE : TaskOverviewUiSupport.BUTTON_DISABLED_TEXTURE)
                .hoverTexture(active ? TaskOverviewUiSupport.BUTTON_HOVER_TEXTURE : TaskOverviewUiSupport.BUTTON_DISABLED_TEXTURE)
                .pressedTexture(active ? TaskOverviewUiSupport.BUTTON_PRESSED_TEXTURE : TaskOverviewUiSupport.BUTTON_DISABLED_TEXTURE)
        );
    }

    private static Label centeredTitle(Component text) {
        Label label = TaskOverviewUiSupport.lineLabel(text, UIScreenTheme.OtherContent.GACHA_TITLE_TEXT);
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
        );
        return label;
    }

    private static Label centeredInfoLabel(Component text, int color) {
        Label label = TaskOverviewUiSupport.lineLabel(text, color);
        label.layout(layout -> layout.widthPercent(100));
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
        );
        return label;
    }

    private static Label sidebarLabel(Component text) {
        Label label = TaskOverviewUiSupport.lineLabel(text, UIScreenTheme.OtherContent.GACHA_SIDEBAR_LABEL_TEXT);
        label.textStyle(style -> style.textWrap(TextWrap.HIDE));
        return label;
    }

    private static Button button(Component text, int width, boolean active, Runnable action) {
        Button button = TaskOverviewUiSupport.createButton(text, width, active);
        button.setOnClick(event -> {
            if (button.isActive()) {
                action.run();
            }
        });
        return button;
    }
}
