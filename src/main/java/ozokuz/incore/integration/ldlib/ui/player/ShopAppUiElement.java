package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopDetailsPresentationMode;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;

final class ShopAppUiElement extends UIElement implements IBindable<String>, PlayerStatusRouteEscapeHandler {
    private String currentJson = "";
    private ShopService.ScreenData data = ShopAppUiSupport.emptyData();
    private final ShopAppUiState state = new ShopAppUiState();
    private final ShopAppLayoutContext layoutContext = new LayoutContext();

    ShopAppUiElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        state.setVisibleOfferRows(ShopAppUiSupport.VISIBLE_OFFER_ROWS);
        rebuild();
    }

    @Override
    public String getValue() {
        return currentJson;
    }

    @Override
    public ShopAppUiElement setValue(@Nullable String value) {
        String nextJson = value == null ? "" : value;
        if (nextJson.equals(currentJson) && !getChildren().isEmpty()) {
            return this;
        }
        currentJson = nextJson;
        data = ShopAppUiSupport.parse(nextJson);
        state.reconcile(data);
        rebuild();
        return this;
    }

    @Override
    public boolean consumeEscape() {
        if (!state.consumeEscape()) {
            return false;
        }
        rebuild();
        return true;
    }

    private void rebuild() {
        clearAllChildren();
        ShopAppLayout shopLayout = ShopAppUiSupport.layoutFor(data, state.activeTab());
        state.setVisibleOfferRows(shopLayout.visibleOfferRows());
        state.reconcile(data);
        ShopAppUiSupport.TabTheme theme = ShopAppUiSupport.themeFor(data, state.activeTab());

        addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                            layout.justifyContent(AlignContent.CENTER);
                            layout.alignItems(AlignItems.CENTER);
                        })
                        .style(style -> style.backgroundTexture(ShopAppUiSupport.flatTexture(theme.backdropFill())))
                        .addChild(createWindow(theme))
        );
    }

    private UIElement createWindow(ShopAppUiSupport.TabTheme theme) {
        var window = INCoreLdLibUiScaffold.createPlainWindowShell(ShopAppUiSupport.TARGET_WINDOW_WIDTH, ShopAppUiSupport.TARGET_WINDOW_HEIGHT);
        window.window().layout(layout -> {
            layout.widthPercent(96);
            layout.heightPercent(94);
            layout.maxWidth(ShopAppUiSupport.TARGET_WINDOW_WIDTH);
            layout.maxHeight(ShopAppUiSupport.TARGET_WINDOW_HEIGHT);
            layout.minWidth(ShopAppUiSupport.MIN_WINDOW_WIDTH);
            layout.minHeight(ShopAppUiSupport.MIN_WINDOW_HEIGHT);
        });
        window.window().style(style -> style.backgroundTexture(ShopAppUiSupport.framedTexture(theme.shellFill(), theme.shellBorder())));

        window.header().layout(layout -> layout.paddingAll(6));
        window.header().style(style -> style.backgroundTexture(ShopAppUiSupport.softTexture(theme.headerFill())));
        window.header().addChildren(createHeader(theme));
        window.body().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        window.body().addChildren(
                createContentRow(theme),
                createFooter(theme)
        );

        if (state.detailsModalOpen() && ShopAppUiSupport.detailsModeFor(data, state.activeTab()) == ShopDetailsPresentationMode.MODAL_OVERLAY) {
            window.root().addChild(ShopAppDetailsModalHost.create(layoutContext, theme));
        }

        return window.root();
    }

    private UIElement createHeader(ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(8);
        });

        UIElement titleBlock = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        titleBlock.addChildren(
                ShopAppUiSupport.heading(Component.translatable("screen.incore.shop.title"), theme.primaryText()),
                createTabs(theme)
        );

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, state.selectedCategoryId());
        ShopService.CurrencyView currency = category == null ? ShopAppUiSupport.emptyCurrencyView() : category.currency();
        UIElement balancePanel = ShopAppUiSupport.highlightedSurface(theme, 8).layout(layout -> {
            layout.widthAuto();
            layout.minWidth(132);
        });
        balancePanel.addChildren(
                ShopAppUiSupport.heading(Component.translatable("screen.incore.shop.balance_label"), theme.secondaryText()),
                ShopAppUiSupport.heading(Component.literal(ShopAppUiSupport.availableCurrencyLabel(currency)), theme.priceText())
        );

        row.addChildren(titleBlock, balancePanel);
        return row;
    }

    private UIElement createTabs(ShopAppUiSupport.TabTheme theme) {
        UIElement strip = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
        });
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(24);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(1);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.alignItems(AlignItems.FLEX_END);
        });
        for (ShopService.TabView tab : ShopAppUiSupport.orderedTabs(data)) {
            ShopTabId tabId = ShopTabId.fromString(tab.tabId());
            boolean active = state.activeTab() == tabId;
            Button button = ShopAppUiSupport.tabButton(Component.literal(tab.displayName()), theme, active);
            button.layout(layout -> {
                layout.flex(1);
                layout.minWidth(72);
            });
            button.setOnClick(event -> {
                state.selectTab(tabId, data);
                rebuild();
            });
            row.addChild(button);
        }
        strip.addChild(row);
        return strip;
    }

    private UIElement createContentRow(ShopAppUiSupport.TabTheme theme) {
        return ShopAppUiSupport.layoutFor(data, state.activeTab()).createContentRow(layoutContext, theme);
    }

    private UIElement createFooter(ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });

        Button done = ShopAppUiSupport.actionButton(Component.translatable("gui.done"), theme, 20);
        done.layout(layout -> layout.width(72));
        done.setOnClick(event -> PacketDistributor.sendToServer(RequestBackIncoreUiPayload.INSTANCE));
        row.addChildren(done, ShopAppUiSupport.spacer());
        return row;
    }

    private final class LayoutContext implements ShopAppLayoutContext {
        @Override
        public ShopService.ScreenData data() {
            return data;
        }

        @Override
        public ShopAppUiState state() {
            return state;
        }

        @Override
        public void rebuild() {
            ShopAppUiElement.this.rebuild();
        }
    }
}
