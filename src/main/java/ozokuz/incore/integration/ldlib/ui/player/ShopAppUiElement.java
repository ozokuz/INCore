package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.sync.bindings.IBindable;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;
import ozokuz.incore.features.shop.network.ShopNetworking;
import ozokuz.incore.integration.ldlib.ui.INCoreLdLibUiScaffold;
import ozokuz.incore.integration.ldlib.ui.RequestBackIncoreUiPayload;

final class ShopAppUiElement extends UIElement implements IBindable<String>, PlayerStatusRouteEscapeHandler {
    private String currentJson = "";
    private ShopService.ScreenData data = ShopAppUiSupport.emptyData();
    private final ShopAppUiState state = new ShopAppUiState();

    ShopAppUiElement() {
        layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        state.setVisibleOfferRows(ShopAppUiSupport.VISIBLE_OFFER_ROWS);
        internalSetup();
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
        state.setVisibleOfferRows(ShopAppUiSupport.visibleOfferRowsFor(state.activeTab()));
        state.reconcile(data);
        ShopAppUiSupport.TabTheme theme = ShopAppUiSupport.themeFor(state.activeTab());
        addChild(
                new UIElement()
                        .layout(layout -> {
                            layout.widthPercent(100);
                            layout.heightPercent(100);
                            layout.justifyContent(AlignContent.CENTER);
                            layout.alignItems(AlignItems.CENTER);
                        })
                        .style(style -> style.backgroundTexture(ShopAppUiSupport.buttonTexture(theme.backdropFill(), theme.panelFill(), theme.panelBorder(), 0)))
                        .addChild(createWindow(theme))
        );
    }

    private UIElement createWindow(ShopAppUiSupport.TabTheme theme) {
        var window = INCoreLdLibUiScaffold.createWindowShell(ShopAppUiSupport.TARGET_WINDOW_WIDTH, ShopAppUiSupport.TARGET_WINDOW_HEIGHT);
        window.window().layout(layout -> {
            layout.widthPercent(96);
            layout.heightPercent(94);
            layout.maxWidth(ShopAppUiSupport.TARGET_WINDOW_WIDTH);
            layout.maxHeight(ShopAppUiSupport.TARGET_WINDOW_HEIGHT);
            layout.minWidth(ShopAppUiSupport.MIN_WINDOW_WIDTH);
            layout.minHeight(ShopAppUiSupport.MIN_WINDOW_HEIGHT);
        });
        window.window().style(style -> style.backgroundTexture(ShopAppUiSupport.buttonTexture(theme.panelFill(), theme.panelBorder(), theme.panelEdge(), 2)));

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

        UIElement balancePanel = ShopAppUiSupport.surface(theme, 8, 1).layout(layout -> {
            layout.widthAuto();
            layout.minWidth(132);
        });
        balancePanel.addChildren(
                ShopAppUiSupport.heading(Component.translatable("screen.incore.shop.balance_label"), theme.secondaryText()),
                ShopAppUiSupport.heading(Component.translatable("screen.incore.shop.balance", data.balanceSpur()), theme.priceText())
        );

        row.addChildren(titleBlock, balancePanel);
        return row;
    }

    private UIElement createTabs(ShopAppUiSupport.TabTheme theme) {
        UIElement strip = new UIElement();
        strip.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(0);
        });

        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(24);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(1);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.alignItems(AlignItems.FLEX_END);
        });
        for (ShopTabId tabId : ShopTabId.values()) {
            boolean active = state.activeTab() == tabId;
            UIElement slot = new UIElement().layout(layout -> {
                layout.flexGrow(0);
                layout.heightPercent(100);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.justifyContent(AlignContent.FLEX_END);
                layout.alignItems(AlignItems.FLEX_START);
            });

            Button button = ShopAppUiSupport.tabButton(tabId.displayName(), theme, active);
            button.layout(layout -> layout.minWidth(96));
            button.setOnClick(event -> {
                state.selectTab(tabId, data);
                rebuild();
            });
            slot.addChild(button);
            row.addChild(slot);
        }

        UIElement underline = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.height(1);
        });
        underline.style(style -> style.backgroundTexture(ShopAppUiSupport.buttonTexture(theme.panelBorder(), theme.panelBorder(), theme.panelBorder(), 0)));

        strip.addChildren(row, underline);
        return strip;
    }

    private UIElement createContentRow(ShopAppUiSupport.TabTheme theme) {
        return switch (ShopAppUiSupport.layoutStyleFor(state.activeTab())) {
            case INDUSTRIAL -> createIndustrialContentRow(theme);
            case LUXURY -> createLuxuryContentRow(theme);
            case ARCADE -> createArcadeContentRow(theme);
        };
    }

    private UIElement createIndustrialContentRow(ShopAppUiSupport.TabTheme theme) {
        return baseContentRow()
                .addChildren(
                        createCategorySidebar(theme).layout(layout -> {
                            layout.flexBasis(144);
                            layout.flexGrow(0);
                            layout.heightPercent(100);
                            layout.minWidth(128);
                            layout.maxWidth(156);
                        }),
                        createOfferColumn(theme).layout(layout -> {
                            layout.flexBasisPercent(44);
                            layout.flexGrow(1);
                            layout.heightPercent(100);
                            layout.minWidth(0);
                        }),
                        createWorkspaceColumn(theme).layout(layout -> {
                            layout.flexBasisPercent(36);
                            layout.flexGrow(1);
                            layout.heightPercent(100);
                            layout.minWidth(0);
                        })
                );
    }

    private UIElement createLuxuryContentRow(ShopAppUiSupport.TabTheme theme) {
        return baseContentRow()
                .addChildren(
                        createCategorySidebar(theme).layout(layout -> {
                            layout.flexBasis(136);
                            layout.flexGrow(0);
                            layout.heightPercent(100);
                            layout.minWidth(124);
                            layout.maxWidth(148);
                        }),
                        createWorkspaceColumn(theme).layout(layout -> {
                            layout.flexBasisPercent(42);
                            layout.flexGrow(1);
                            layout.heightPercent(100);
                            layout.minWidth(0);
                        }),
                        createOfferColumn(theme).layout(layout -> {
                            layout.flexBasisPercent(34);
                            layout.flexGrow(1);
                            layout.heightPercent(100);
                            layout.minWidth(0);
                        })
                );
    }

    private UIElement createArcadeContentRow(ShopAppUiSupport.TabTheme theme) {
        UIElement rightColumn = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.heightPercent(100);
            layout.minWidth(0);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        rightColumn.addChildren(
                createWorkspaceColumn(theme).layout(layout -> {
                    layout.flexBasisPercent(44);
                    layout.flexGrow(1);
                    layout.minHeight(0);
                }),
                createOfferColumn(theme).layout(layout -> {
                    layout.flexBasisPercent(56);
                    layout.flexGrow(1);
                    layout.minHeight(0);
                })
        );

        return baseContentRow()
                .addChildren(
                        createCategorySidebar(theme).layout(layout -> {
                            layout.flexBasis(140);
                            layout.flexGrow(0);
                            layout.heightPercent(100);
                            layout.minWidth(124);
                            layout.maxWidth(152);
                        }),
                        rightColumn
                );
    }

    private UIElement baseContentRow() {
        return new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.widthPercent(100);
                    layout.minHeight(0);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(8);
                });
    }

    private UIElement createCategorySidebar(ShopAppUiSupport.TabTheme theme) {
        UIElement sidebar = ShopAppUiSupport.surface(theme, 6, 1);
        sidebar.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        sidebar.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.categories_heading"),
                        ShopAppUiSupport.sidebarSubtitleFor(state.activeTab()),
                        theme
                ),
                createCategoryList(theme)
        );
        return sidebar;
    }

    private UIElement createCategoryList(ShopAppUiSupport.TabTheme theme) {
        UIElement list = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });

        List<ShopService.CategoryView> categories = ShopAppUiSupport.categoriesForTab(data, state.activeTab());
        if (categories.isEmpty()) {
            list.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_categories"), theme.secondaryText()));
            return list;
        }

        for (ShopService.CategoryView category : categories) {
            boolean active = category.categoryId().equals(state.selectedCategoryId());
            Button button = ShopAppUiSupport.chipButton(Component.literal(category.displayName()), theme, active);
            button.layout(layout -> layout.widthPercent(100));
            button.setOnClick(event -> {
                state.selectCategory(category.categoryId(), data);
                rebuild();
            });
            list.addChild(button);
        }
        return list;
    }

    private UIElement createOfferColumn(ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.surface(theme, 6, 1);
        column.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, state.selectedCategoryId());
        String stockText = category == null ? Component.translatable("screen.incore.shop.no_categories").getString()
                : ShopAppUiSupport.stockLabel(category.availableStock());

        column.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.offers_heading"),
                        ShopAppUiSupport.offersSubtitleFor(state.activeTab(), stockText),
                        theme
                ),
                createOfferList(theme),
                createPager(theme)
        );
        return column;
    }

    private UIElement createOfferList(ShopAppUiSupport.TabTheme theme) {
        UIElement list = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });

        List<ShopService.OfferView> offers = state.visibleOffers(data);
        if (offers.isEmpty()) {
            list.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return list;
        }

        for (ShopService.OfferView offer : offers) {
            list.addChild(createOfferCard(offer, theme));
        }
        return list;
    }

    private UIElement createOfferCard(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        ShopAppUiSupport.LayoutStyle layoutStyle = ShopAppUiSupport.layoutStyleFor(state.activeTab());
        boolean selected = offer.offerId().equals(state.selectedOfferId());
        Button card = new Button().setText(Component.empty());
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.height(ShopAppUiSupport.offerCardHeight(state.activeTab()));
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
            layout.paddingAll(6);
        });
        card.text.setDisplay(false);
        card.buttonStyle(style -> style
                .baseTexture(
                        selected
                                ? ShopAppUiSupport.buttonTexture(theme.rowHover(), theme.accent(), theme.primaryText(), 1)
                                : ShopAppUiSupport.buttonTexture(theme.panelFill(), theme.panelBorder(), theme.panelEdge(), 1)
                )
                .hoverTexture(ShopAppUiSupport.buttonTexture(theme.rowHover(), theme.accent(), theme.primaryText(), 1))
                .pressedTexture(ShopAppUiSupport.buttonTexture(theme.panelFill(), theme.accent(), theme.primaryText(), 1))
        );
        switch (layoutStyle) {
            case INDUSTRIAL -> card.addChildren(
                    ShopAppUiSupport.itemIcon(
                            ShopAppUiSupport.stackForOffer(offer),
                            ShopAppUiSupport.offerIconSize(state.activeTab()),
                            Component.literal(offer.displayName())
                    ),
                    offerTextColumn(offer, theme),
                    offerPriceColumn(offer, theme)
            );
            case LUXURY -> card.addChildren(
                    ShopAppUiSupport.itemIcon(
                            ShopAppUiSupport.stackForOffer(offer),
                            ShopAppUiSupport.offerIconSize(state.activeTab()),
                            Component.literal(offer.displayName())
                    ),
                    luxuryOfferTextColumn(offer, theme),
                    luxuryOfferBadgeColumn(offer, theme)
            );
            case ARCADE -> card.addChildren(
                    ShopAppUiSupport.itemIcon(
                            ShopAppUiSupport.stackForOffer(offer),
                            ShopAppUiSupport.offerIconSize(state.activeTab()),
                            Component.literal(offer.displayName())
                    ),
                    arcadeOfferTextColumn(offer, theme),
                    arcadeOfferBadgeColumn(offer, theme)
            );
            default -> throw new IllegalStateException("Unhandled shop layout style: " + layoutStyle);
        }
        card.setOnClick(event -> {
            state.openPurchase(offer.offerId(), data);
            rebuild();
        });
        return card;
    }

    private UIElement offerTextColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(1);
        });

        Label name = ShopAppUiSupport.heading(Component.literal(offer.displayName()), offer.locked() ? theme.secondaryText() : theme.primaryText());
        name.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );

        String secondary = offer.locked()
                ? Component.translatable("screen.incore.shop.locked_short").getString()
                : ShopAppUiSupport.bundleLabel(offer.itemCount());
        Label meta = ShopAppUiSupport.heading(Component.literal(secondary), offer.locked() ? theme.alertText() : theme.secondaryText());
        meta.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );

        column.addChildren(name, meta);
        return column;
    }

    private UIElement offerPriceColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(112);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.gapAll(1);
        });

        Label price = ShopAppUiSupport.heading(Component.literal(offer.priceSpur() + " spur"), theme.priceText());
        price.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.RIGHT)
        );

        Label stock = ShopAppUiSupport.heading(Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme.secondaryText());
        stock.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.RIGHT)
        );

        column.addChildren(price, stock);
        return column;
    }

    private UIElement luxuryOfferTextColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });

        Label name = ShopAppUiSupport.heading(Component.literal(offer.displayName()), theme.primaryText());
        name.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );

        UIElement metaRow = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(4);
        });
        metaRow.addChildren(
                compactPill(Component.literal(ShopAppUiSupport.bundleLabel(offer.itemCount())), theme.secondaryText(), theme.panelEdge(), theme),
                compactPill(Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme.priceText(), theme.panelBorder(), theme)
        );

        column.addChildren(name, metaRow);
        return column;
    }

    private UIElement luxuryOfferBadgeColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(98);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(4);
        });
        column.addChildren(
                compactPill(Component.literal(offer.priceSpur() + " spur"), theme.priceText(), theme.accent(), theme),
                compactPill(
                        Component.translatable(offer.locked() ? "screen.incore.shop.locked_short" : "screen.incore.shop.select"),
                        offer.locked() ? theme.alertText() : theme.primaryText(),
                        offer.locked() ? theme.alertText() : theme.panelBorder(),
                        theme
                )
        );
        return column;
    }

    private UIElement arcadeOfferTextColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });

        Label name = ShopAppUiSupport.heading(Component.literal(offer.displayName()), theme.primaryText());
        name.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );
        Label bundle = ShopAppUiSupport.heading(Component.literal(ShopAppUiSupport.bundleLabel(offer.itemCount())), theme.secondaryText());
        bundle.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );
        column.addChildren(name, bundle);
        return column;
    }

    private UIElement arcadeOfferBadgeColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(104);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(4);
        });
        column.addChildren(
                loudBadge(Component.literal(offer.priceSpur() + " spur"), theme.priceText(), theme.accent(), theme),
                loudBadge(Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme.primaryText(), theme.rowHover(), theme)
        );
        return column;
    }

    private UIElement createPager(ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });

        Button previous = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.prev"), theme, 20);
        previous.layout(layout -> layout.width(72));
        previous.setActive(state.canScrollPrevious());
        previous.setOnClick(event -> {
            state.scrollBy(-1, data);
            rebuild();
        });

        Button next = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.next"), theme, 20);
        next.layout(layout -> layout.width(72));
        next.setActive(state.canScrollNext(data));
        next.setOnClick(event -> {
            state.scrollBy(1, data);
            rebuild();
        });

        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(data, state.selectedCategoryId());
        int total = offers.size();
        int current = total == 0 ? 0 : Math.min(total, state.offerScrollRow() + 1);
        Label counter = ShopAppUiSupport.heading(Component.literal(current + "/" + Math.max(1, total)), theme.secondaryText());
        counter.layout(layout -> layout.flex(1));
        counter.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));

        row.addChildren(previous, counter, next);
        return row;
    }

    private UIElement createWorkspaceColumn(ShopAppUiSupport.TabTheme theme) {
        return state.purchaseWorkspaceOpen() ? createPurchaseWorkspace(theme) : createStandbyWorkspace(theme);
    }

    private UIElement createStandbyWorkspace(ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.surface(theme, 8, 1);
        column.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(6);
        });

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, state.selectedCategoryId());
        String categoryName = category == null ? Component.translatable("screen.incore.shop.no_categories").getString() : category.displayName();
        String stockText = category == null ? Component.translatable("screen.incore.shop.no_categories").getString() : ShopAppUiSupport.stockLabel(category.availableStock());

        UIElement top = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        top.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.workspace_heading"),
                        Component.literal(categoryName),
                        theme
                ),
                metricRow(Component.translatable("screen.incore.shop.workspace.stock_bucket"), Component.literal(stockText), theme),
                metricRow(
                        Component.translatable("screen.incore.shop.workspace.available_offers"),
                        Component.literal(Integer.toString(ShopAppUiSupport.offersForCategory(data, state.selectedCategoryId()).size())),
                        theme
                )
        );
        column.addChildren(top, standbyAccentPanel(theme));
        return column;
    }

    private UIElement createPurchaseWorkspace(ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.surface(theme, 8, 1);
        column.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(6);
        });

        ShopService.OfferView offer = state.selectedOffer(data);
        if (offer == null) {
            return createStandbyWorkspace(theme);
        }

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(data, offer.categoryId());
        int totalCost = offer.priceSpur() * state.quantity();
        boolean canPurchase = !offer.locked() && (offer.availableStock() < 0 || offer.availableStock() > 0);

        UIElement top = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        top.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.purchase_workspace"),
                        Component.literal(offer.displayName()),
                        theme
                ),
                createHero(offer, theme),
                metricRow(Component.translatable("screen.incore.shop.category", category == null ? "" : category.displayName()), Component.literal(""), theme),
                metricRow(Component.translatable("screen.incore.shop.price_each", offer.priceSpur()), Component.literal(""), theme),
                metricRow(Component.translatable("screen.incore.shop.bundle", offer.itemCount()), Component.literal(""), theme),
                metricRow(Component.translatable("screen.incore.shop.stock_label"), Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme),
                createQuantityControls(theme),
                metricRow(Component.translatable("screen.incore.shop.total_cost", totalCost), Component.literal(""), theme)
        );
        if (offer.locked()) {
            top.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.locked"), theme.alertText()));
        } else if (offer.availableStock() == 0) {
            top.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.out_of_stock_inline"), theme.alertText()));
        }

        UIElement bottom = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });

        Button close = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.close_workspace"), theme, 20);
        close.layout(layout -> layout.width(98));
        close.setOnClick(event -> {
            state.closePurchase(data);
            rebuild();
        });

        Button purchase = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.purchase"), theme, 20);
        purchase.layout(layout -> {
            layout.width(118);
            layout.flexGrow(0);
        });
        purchase.setActive(canPurchase);
        purchase.setOnClick(event -> {
            if (!canPurchase) {
                return;
            }
            var offerId = state.selectedOfferResource();
            var categoryId = state.selectedCategoryResource();
            if (offerId != null) {
                ShopNetworking.sendPurchase(offerId, state.quantity(), categoryId);
            }
        });
        bottom.addChildren(close, purchase);

        column.addChildren(top, bottom);
        return column;
    }

    private UIElement createHero(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement hero = ShopAppUiSupport.surface(theme, 6, 1);
        hero.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        hero.addChildren(
                ShopAppUiSupport.itemIcon(
                        ShopAppUiSupport.stackForOffer(offer),
                        ShopAppUiSupport.layoutStyleFor(state.activeTab()) == ShopAppUiSupport.LayoutStyle.LUXURY ? 26 : 20,
                        Component.literal(offer.displayName()),
                        Component.literal(ShopAppUiSupport.priceLabel(offer.priceSpur()))
                ),
                new UIElement()
                        .layout(layout -> {
                            layout.flex(1);
                            layout.minWidth(0);
                            layout.flexDirection(FlexDirection.COLUMN);
                            layout.gapAll(1);
                        })
                        .addChildren(
                                ShopAppUiSupport.heading(Component.literal(offer.displayName()), theme.primaryText()),
                                ShopAppUiSupport.bodyLabel(Component.literal(ShopAppUiSupport.bundleLabel(offer.itemCount())), theme.secondaryText())
                        )
        );
        return hero;
    }

    private UIElement createQuantityControls(ShopAppUiSupport.TabTheme theme) {
        UIElement row = ShopAppUiSupport.surface(theme, 6, 1);
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(6);
        });

        Button decrease = ShopAppUiSupport.actionButton(Component.literal("-"), theme, 18);
        decrease.layout(layout -> layout.width(24));
        decrease.setActive(state.quantity() > 1);
        decrease.setOnClick(event -> {
            state.decreaseQuantity();
            rebuild();
        });

        Label quantity = ShopAppUiSupport.heading(Component.translatable("screen.incore.shop.quantity", state.quantity()), theme.primaryText());
        quantity.layout(layout -> layout.flex(1));
        quantity.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));

        Button increase = ShopAppUiSupport.actionButton(Component.literal("+"), theme, 18);
        increase.layout(layout -> layout.width(24));
        increase.setActive(state.quantity() < state.quantityMax(data));
        increase.setOnClick(event -> {
            state.increaseQuantity(data);
            rebuild();
        });

        row.addChildren(decrease, quantity, increase);
        return row;
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

    private UIElement sectionHeader(Component title, Component subtitle, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(1);
        });
        row.addChildren(
                ShopAppUiSupport.heading(title, theme.primaryText()),
                ShopAppUiSupport.heading(subtitle, theme.secondaryText())
        );
        return row;
    }

    private UIElement metricRow(Component left, Component right, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        Label leftLabel = ShopAppUiSupport.heading(left, theme.secondaryText());
        leftLabel.layout(layout -> layout.flex(1));
        leftLabel.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );
        row.addChild(leftLabel);
        if (!right.getString().isBlank()) {
            Label rightLabel = ShopAppUiSupport.heading(right, theme.primaryText());
            rightLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
            row.addChild(rightLabel);
        }
        return row;
    }

    private UIElement compactPill(Component text, int textColor, int borderColor, ShopAppUiSupport.TabTheme theme) {
        UIElement pill = ShopAppUiSupport.surface(theme, 4, 1);
        pill.layout(layout -> {
            layout.widthAuto();
            layout.paddingHorizontal(4);
        });
        pill.style(style -> style.backgroundTexture(ShopAppUiSupport.buttonTexture(theme.panelEdge(), borderColor, borderColor, 1)));
        pill.addChild(ShopAppUiSupport.heading(text, textColor).textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER)));
        return pill;
    }

    private UIElement loudBadge(Component text, int textColor, int fillColor, ShopAppUiSupport.TabTheme theme) {
        UIElement badge = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.paddingVertical(3);
            layout.paddingHorizontal(4);
        });
        badge.style(style -> style.backgroundTexture(ShopAppUiSupport.buttonTexture(fillColor, theme.accent(), theme.primaryText(), 1)));
        Label label = ShopAppUiSupport.heading(text, textColor);
        label.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));
        badge.addChild(label);
        return badge;
    }

    private UIElement standbyAccentPanel(ShopAppUiSupport.TabTheme theme) {
        return switch (ShopAppUiSupport.layoutStyleFor(state.activeTab())) {
            case INDUSTRIAL -> metricRow(Component.translatable("screen.incore.shop.workspace.focus"), Component.translatable("screen.incore.shop.ready"), theme);
            case LUXURY -> compactPill(Component.translatable("screen.incore.shop.curated"), theme.priceText(), theme.accent(), theme);
            case ARCADE -> loudBadge(Component.translatable("screen.incore.shop.choose_offer"), theme.primaryText(), theme.accent(), theme);
        };
    }
}
