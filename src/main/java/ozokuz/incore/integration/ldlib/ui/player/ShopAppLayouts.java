package ozokuz.incore.integration.ldlib.ui.player;

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
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;
import ozokuz.incore.features.shop.network.ShopNetworking;

final class ShopAppLayouts {
    private static final ShopAppItemLayout INDUSTRIAL_ITEM = (card, offer, context, theme, iconSize) -> card.addChildren(
            ShopAppUiSupport.itemIcon(
                    ShopAppUiSupport.stackForOffer(offer),
                    iconSize,
                    Component.literal(offer.displayName())
            ),
            industrialOfferTextColumn(offer, theme),
            offerPriceColumn(offer, theme)
    );

    private static final ShopAppItemLayout LUXURY_ITEM = (card, offer, context, theme, iconSize) -> card.addChildren(
            ShopAppUiSupport.itemIcon(
                    ShopAppUiSupport.stackForOffer(offer),
                    iconSize,
                    Component.literal(offer.displayName())
            ),
            luxuryOfferTextColumn(offer, theme),
            luxuryOfferBadgeColumn(offer, theme)
    );

    private static final ShopAppItemLayout ARCADE_ITEM = (card, offer, context, theme, iconSize) -> card.addChildren(
            ShopAppUiSupport.itemIcon(
                    ShopAppUiSupport.stackForOffer(offer),
                    iconSize,
                    Component.literal(offer.displayName())
            ),
            arcadeOfferTextColumn(offer, theme),
            arcadeOfferBadgeColumn(offer, theme)
    );

    private static final ShopAppLayout INDUSTRIAL = new BasicLayout(8, 44, 16, 20, INDUSTRIAL_ITEM) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            return baseContentRow()
                    .addChildren(
                            createCategorySidebar(context, theme).layout(layout -> {
                                layout.flexBasis(144);
                                layout.flexGrow(0);
                                layout.heightPercent(100);
                                layout.minWidth(128);
                                layout.maxWidth(156);
                            }),
                            createOfferColumn(context, theme).layout(layout -> {
                                layout.flexBasisPercent(44);
                                layout.flexGrow(1);
                                layout.heightPercent(100);
                                layout.minWidth(0);
                            }),
                            createWorkspaceColumn(context, theme).layout(layout -> {
                                layout.flexBasisPercent(36);
                                layout.flexGrow(1);
                                layout.heightPercent(100);
                                layout.minWidth(0);
                            })
                    );
        }

        @Override
        public UIElement createStandbyAccent(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            return metricRow(Component.translatable("screen.incore.shop.workspace.focus"), Component.translatable("screen.incore.shop.ready"), theme);
        }
    };

    private static final ShopAppLayout LUXURY = new BasicLayout(7, 54, 20, 26, LUXURY_ITEM) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            return baseContentRow()
                    .addChildren(
                            createCategorySidebar(context, theme).layout(layout -> {
                                layout.flexBasis(136);
                                layout.flexGrow(0);
                                layout.heightPercent(100);
                                layout.minWidth(124);
                                layout.maxWidth(148);
                            }),
                            createWorkspaceColumn(context, theme).layout(layout -> {
                                layout.flexBasisPercent(42);
                                layout.flexGrow(1);
                                layout.heightPercent(100);
                                layout.minWidth(0);
                            }),
                            createOfferColumn(context, theme).layout(layout -> {
                                layout.flexBasisPercent(34);
                                layout.flexGrow(1);
                                layout.heightPercent(100);
                                layout.minWidth(0);
                            })
                    );
        }

        @Override
        public UIElement createStandbyAccent(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            return compactPill(Component.translatable("screen.incore.shop.curated"), theme.priceText(), theme.accent(), theme);
        }
    };

    private static final ShopAppLayout ARCADE = new BasicLayout(6, 50, 18, 20, ARCADE_ITEM) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement rightColumn = new UIElement().layout(layout -> {
                layout.flex(1);
                layout.heightPercent(100);
                layout.minWidth(0);
                layout.minHeight(0);
                layout.flexDirection(FlexDirection.COLUMN);
                layout.gapAll(8);
            });
            rightColumn.addChildren(
                    createWorkspaceColumn(context, theme).layout(layout -> {
                        layout.flexBasisPercent(44);
                        layout.flexGrow(1);
                        layout.minHeight(0);
                    }),
                    createOfferColumn(context, theme).layout(layout -> {
                        layout.flexBasisPercent(56);
                        layout.flexGrow(1);
                        layout.minHeight(0);
                    })
            );

            return baseContentRow()
                    .addChildren(
                            createCategorySidebar(context, theme).layout(layout -> {
                                layout.flexBasis(140);
                                layout.flexGrow(0);
                                layout.heightPercent(100);
                                layout.minWidth(124);
                                layout.maxWidth(152);
                            }),
                            rightColumn
                    );
        }

        @Override
        public UIElement createStandbyAccent(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            return loudBadge(Component.translatable("screen.incore.shop.choose_offer"), theme.primaryText(), theme.accent(), theme);
        }
    };

    private ShopAppLayouts() {
    }

    static ShopAppLayout forTab(ShopTabId tabId) {
        return switch (tabId) {
            case SUPPLIES -> INDUSTRIAL;
            case ROTATIONS -> LUXURY;
            case CACHES -> ARCADE;
        };
    }

    private static UIElement baseContentRow() {
        return new UIElement()
                .layout(layout -> {
                    layout.flex(1);
                    layout.widthPercent(100);
                    layout.minHeight(0);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(8);
                });
    }

    private static UIElement createCategorySidebar(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement sidebar = ShopAppUiSupport.surface(theme, 6, 1);
        sidebar.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        ShopService.CategoryView selectedCategory = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        String subtitle = selectedCategory == null
                ? ShopAppUiSupport.sidebarSubtitleFor(context.state().activeTab()).getString()
                : ShopAppUiSupport.availableCurrencyLabel(selectedCategory.currency());
        sidebar.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.categories_heading"),
                        Component.literal(subtitle),
                        theme
                ),
                createCategoryList(context, theme)
        );
        return sidebar;
    }

    private static UIElement createCategoryList(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement list = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });

        List<ShopService.CategoryView> categories = ShopAppUiSupport.categoriesForTab(context.data(), context.state().activeTab());
        if (categories.isEmpty()) {
            list.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_categories"), theme.secondaryText()));
            return list;
        }

        for (ShopService.CategoryView category : categories) {
            boolean active = category.categoryId().equals(context.state().selectedCategoryId());
            Button button = ShopAppUiSupport.chipButton(Component.literal(category.displayName()), theme, active);
            button.layout(layout -> layout.widthPercent(100));
            button.setOnClick(event -> {
                context.state().selectCategory(category.categoryId(), context.data());
                context.rebuild();
            });
            list.addChild(button);
        }
        return list;
    }

    private static UIElement createOfferColumn(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.surface(theme, 6, 1);
        column.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        String subtitle = category == null
                ? Component.translatable("screen.incore.shop.no_categories").getString()
                : category.rotating()
                ? ShopAppUiSupport.rotationRemainingLabel(category.rotationRemainingMillis())
                : ShopAppUiSupport.stockLabel(category.availableStock());

        column.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.offers_heading"),
                        Component.literal(subtitle),
                        theme
                ),
                createOfferList(context, theme),
                createPager(context, theme)
        );
        return column;
    }

    private static UIElement createOfferList(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement list = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });

        List<ShopService.OfferView> offers = context.state().visibleOffers(context.data());
        if (offers.isEmpty()) {
            list.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return list;
        }

        for (ShopService.OfferView offer : offers) {
            list.addChild(createOfferCard(context, offer, theme));
        }
        return list;
    }

    private static UIElement createOfferCard(ShopAppLayoutContext context, ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        ShopAppLayout layout = ShopAppUiSupport.layoutFor(context.state().activeTab());
        boolean selected = offer.offerId().equals(context.state().selectedOfferId());
        Button card = new Button().setText(Component.empty());
        card.layout(layoutSpec -> {
            layoutSpec.widthPercent(100);
            layoutSpec.height(layout.offerCardHeight());
            layoutSpec.flexDirection(FlexDirection.ROW);
            layoutSpec.alignItems(AlignItems.CENTER);
            layoutSpec.gapAll(6);
            layoutSpec.paddingAll(6);
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
        layout.item().addOfferChildren(card, offer, context, theme, layout.offerIconSize());
        card.setOnClick(event -> {
            context.state().openPurchase(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static UIElement createPager(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });

        Button previous = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.prev"), theme, 20);
        previous.layout(layout -> layout.width(72));
        previous.setActive(context.state().canScrollPrevious());
        previous.setOnClick(event -> {
            context.state().scrollBy(-1, context.data());
            context.rebuild();
        });

        Button next = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.next"), theme, 20);
        next.layout(layout -> layout.width(72));
        next.setActive(context.state().canScrollNext(context.data()));
        next.setOnClick(event -> {
            context.state().scrollBy(1, context.data());
            context.rebuild();
        });

        List<ShopService.OfferView> offers = ShopAppUiSupport.offersForCategory(context.data(), context.state().selectedCategoryId());
        int total = offers.size();
        int current = total == 0 ? 0 : Math.min(total, context.state().offerScrollRow() + 1);
        Label counter = ShopAppUiSupport.heading(Component.literal(current + "/" + Math.max(1, total)), theme.secondaryText());
        counter.layout(layout -> layout.flex(1));
        counter.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));

        row.addChildren(previous, counter, next);
        return row;
    }

    private static UIElement createWorkspaceColumn(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        return context.state().purchaseWorkspaceOpen() ? createPurchaseWorkspace(context, theme) : createStandbyWorkspace(context, theme);
    }

    private static UIElement createStandbyWorkspace(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.surface(theme, 8, 1);
        column.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(6);
        });

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        String categoryName = category == null ? Component.translatable("screen.incore.shop.no_categories").getString() : category.displayName();
        String subtitle = category == null
                ? Component.translatable("screen.incore.shop.no_categories").getString()
                : ShopAppUiSupport.availableCurrencyLabel(category.currency());

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
                metricRow(Component.translatable("screen.incore.shop.workspace.currency"), Component.literal(subtitle), theme),
                metricRow(
                        Component.translatable("screen.incore.shop.workspace.available_offers"),
                        Component.literal(Integer.toString(ShopAppUiSupport.offersForCategory(context.data(), context.state().selectedCategoryId()).size())),
                        theme
                )
        );
        if (category != null && category.rotating()) {
            top.addChild(metricRow(
                    Component.translatable("screen.incore.shop.workspace.rotation"),
                    Component.literal(ShopAppUiSupport.rotationRemainingLabel(category.rotationRemainingMillis())),
                    theme
            ));
        } else if (category != null) {
            top.addChild(metricRow(
                    Component.translatable("screen.incore.shop.workspace.stock_bucket"),
                    Component.literal(ShopAppUiSupport.stockLabel(category.availableStock())),
                    theme
            ));
        }
        column.addChildren(top, ShopAppUiSupport.layoutFor(context.state().activeTab()).createStandbyAccent(context, theme));
        return column;
    }

    private static UIElement createPurchaseWorkspace(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.surface(theme, 8, 1);
        column.layout(layout -> {
            layout.heightPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(6);
        });

        ShopService.OfferView offer = context.state().selectedOffer(context.data());
        if (offer == null) {
            return createStandbyWorkspace(context, theme);
        }

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), offer.categoryId());
        int totalCost = offer.price() * context.state().quantity();
        boolean canPurchase = !offer.locked()
                && (offer.availableStock() < 0 || offer.availableStock() > 0)
                && offer.currency().availableAmount() >= totalCost;

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
                createHero(context, offer, theme),
                metricRow(Component.translatable("screen.incore.shop.category", category == null ? "" : category.displayName()), Component.literal(""), theme),
                metricRow(Component.translatable("screen.incore.shop.price_each"), Component.literal(ShopAppUiSupport.currencyAmountLabel(offer.currency())), theme),
                metricRow(Component.translatable("screen.incore.shop.workspace.currency"), Component.literal(ShopAppUiSupport.availableCurrencyLabel(offer.currency())), theme),
                metricRow(Component.translatable("screen.incore.shop.stock_label"), Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme),
                createRewardContents(offer, theme),
                createQuantityControls(context, theme),
                metricRow(Component.translatable("screen.incore.shop.total_cost"), Component.literal(totalCost + " " + offer.currency().label()), theme)
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
            context.state().closePurchase(context.data());
            context.rebuild();
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
            var offerId = context.state().selectedOfferResource();
            var categoryId = context.state().selectedCategoryResource();
            if (offerId != null) {
                ShopNetworking.sendPurchase(offerId, context.state().quantity(), categoryId);
            }
        });
        bottom.addChildren(close, purchase);

        column.addChildren(top, bottom);
        return column;
    }

    private static UIElement createHero(ShopAppLayoutContext context, ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
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
                        ShopAppUiSupport.layoutFor(context.state().activeTab()).heroIconSize(),
                        Component.literal(offer.displayName())
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
                                ShopAppUiSupport.bodyLabel(Component.literal(ShopAppUiSupport.rewardSummary(offer)), theme.secondaryText())
                        )
        );
        return hero;
    }

    private static UIElement createRewardContents(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.surface(theme, 6, 1);
        column.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });
        column.addChild(sectionHeader(
                Component.translatable("screen.incore.shop.reward_contents"),
                Component.literal(ShopAppUiSupport.rewardSummary(offer)),
                theme
        ));
        for (ShopService.RewardEntryView entry : offer.rewardEntries()) {
            column.addChild(rewardRow(entry, theme));
        }
        return column;
    }

    private static UIElement rewardRow(ShopService.RewardEntryView entry, ShopAppUiSupport.TabTheme theme) {
        UIElement row = ShopAppUiSupport.surface(theme, 4, 1);
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        row.addChildren(
                ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackFromRewardEntry(entry), 16, Component.literal(ShopAppUiSupport.rewardEntryLabel(entry))),
                new UIElement().layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                }).addChild(
                        ShopAppUiSupport.heading(Component.literal(ShopAppUiSupport.rewardEntryLabel(entry)), theme.primaryText())
                ),
                ShopAppUiSupport.heading(Component.literal("x" + entry.count()), theme.secondaryText()).textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT))
        );
        return row;
    }

    private static UIElement createQuantityControls(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
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
        decrease.setActive(context.state().quantity() > 1);
        decrease.setOnClick(event -> {
            context.state().decreaseQuantity();
            context.rebuild();
        });

        Label quantity = ShopAppUiSupport.heading(Component.translatable("screen.incore.shop.quantity", context.state().quantity()), theme.primaryText());
        quantity.layout(layout -> layout.flex(1));
        quantity.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));

        Button increase = ShopAppUiSupport.actionButton(Component.literal("+"), theme, 18);
        increase.layout(layout -> layout.width(24));
        increase.setActive(context.state().quantity() < context.state().quantityMax(context.data()));
        increase.setOnClick(event -> {
            context.state().increaseQuantity(context.data());
            context.rebuild();
        });

        row.addChildren(decrease, quantity, increase);
        return row;
    }

    private static UIElement industrialOfferTextColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
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
                : ShopAppUiSupport.rewardSummary(offer);
        Label meta = ShopAppUiSupport.heading(Component.literal(secondary), offer.locked() ? theme.alertText() : theme.secondaryText());
        meta.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );

        column.addChildren(name, meta);
        return column;
    }

    private static UIElement offerPriceColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(132);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.gapAll(1);
        });

        Label price = ShopAppUiSupport.heading(Component.literal(ShopAppUiSupport.currencyAmountLabel(offer.currency())), theme.priceText());
        price.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.RIGHT)
        );

        String secondary = offer.rotationRemainingMillis() >= 0L
                ? ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())
                : ShopAppUiSupport.stockLabel(offer.availableStock());
        Label stock = ShopAppUiSupport.heading(Component.literal(secondary), theme.secondaryText());
        stock.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.RIGHT)
        );

        column.addChildren(price, stock);
        return column;
    }

    private static UIElement luxuryOfferTextColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
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
                compactPill(Component.literal(ShopAppUiSupport.rewardSummary(offer)), theme.secondaryText(), theme.panelEdge(), theme),
                compactPill(Component.literal(ShopAppUiSupport.availableCurrencyLabel(offer.currency())), theme.priceText(), theme.panelBorder(), theme)
        );

        column.addChildren(name, metaRow);
        return column;
    }

    private static UIElement luxuryOfferBadgeColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(116);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(4);
        });
        column.addChildren(
                compactPill(Component.literal(ShopAppUiSupport.currencyAmountLabel(offer.currency())), theme.priceText(), theme.accent(), theme),
                compactPill(
                        Component.literal(
                                offer.rotationRemainingMillis() >= 0L
                                        ? ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())
                                        : ShopAppUiSupport.stockLabel(offer.availableStock())
                        ),
                        offer.locked() ? theme.alertText() : theme.primaryText(),
                        offer.locked() ? theme.alertText() : theme.panelBorder(),
                        theme
                )
        );
        return column;
    }

    private static UIElement arcadeOfferTextColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
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
        Label bundle = ShopAppUiSupport.heading(Component.literal(ShopAppUiSupport.rewardSummary(offer)), theme.secondaryText());
        bundle.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
        );
        column.addChildren(name, bundle);
        return column;
    }

    private static UIElement arcadeOfferBadgeColumn(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(116);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(4);
        });
        column.addChildren(
                loudBadge(Component.literal(ShopAppUiSupport.currencyAmountLabel(offer.currency())), theme.priceText(), theme.accent(), theme),
                loudBadge(
                        Component.literal(
                                offer.rotationRemainingMillis() >= 0L
                                        ? ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())
                                        : ShopAppUiSupport.stockLabel(offer.availableStock())
                        ),
                        theme.primaryText(),
                        theme.rowHover(),
                        theme
                )
        );
        return column;
    }

    private static UIElement sectionHeader(Component title, Component subtitle, ShopAppUiSupport.TabTheme theme) {
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

    private static UIElement metricRow(Component left, Component right, ShopAppUiSupport.TabTheme theme) {
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

    private static UIElement compactPill(Component text, int textColor, int borderColor, ShopAppUiSupport.TabTheme theme) {
        UIElement pill = ShopAppUiSupport.surface(theme, 4, 1);
        pill.layout(layout -> {
            layout.widthAuto();
            layout.paddingHorizontal(4);
        });
        pill.style(style -> style.backgroundTexture(ShopAppUiSupport.buttonTexture(theme.panelEdge(), borderColor, borderColor, 1)));
        pill.addChild(ShopAppUiSupport.heading(text, textColor).textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER)));
        return pill;
    }

    private static UIElement loudBadge(Component text, int textColor, int fillColor, ShopAppUiSupport.TabTheme theme) {
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

    private abstract static class BasicLayout implements ShopAppLayout {
        private final int visibleOfferRows;
        private final int offerCardHeight;
        private final int offerIconSize;
        private final int heroIconSize;
        private final ShopAppItemLayout itemLayout;

        private BasicLayout(int visibleOfferRows, int offerCardHeight, int offerIconSize, int heroIconSize, ShopAppItemLayout itemLayout) {
            this.visibleOfferRows = visibleOfferRows;
            this.offerCardHeight = offerCardHeight;
            this.offerIconSize = offerIconSize;
            this.heroIconSize = heroIconSize;
            this.itemLayout = itemLayout;
        }

        @Override
        public int visibleOfferRows() {
            return visibleOfferRows;
        }

        @Override
        public int offerCardHeight() {
            return offerCardHeight;
        }

        @Override
        public int offerIconSize() {
            return offerIconSize;
        }

        @Override
        public int heroIconSize() {
            return heroIconSize;
        }

        @Override
        public ShopAppItemLayout item() {
            return itemLayout;
        }
    }
}
