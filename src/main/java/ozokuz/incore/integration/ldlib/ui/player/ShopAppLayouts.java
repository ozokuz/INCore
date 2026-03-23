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
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopLayoutId;
import ozokuz.incore.features.shop.ShopService;

final class ShopAppLayouts {
    private static final int INDUSTRIAL_RAIL_ROWS = 3;
    private static final int INDUSTRIAL_BOARD_ROWS = 2;

    private static final ShopAppLayout INDUSTRIAL = new FixedLayout(INDUSTRIAL_RAIL_ROWS + INDUSTRIAL_BOARD_ROWS) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            main.addChildren(
                    sidebar(context, theme).layout(layout -> {
                        layout.heightPercent(100);
                        layout.flexBasis(136);
                        layout.minWidth(128);
                        layout.maxWidth(140);
                    }),
                    industrialCenter(context, theme).layout(layout -> {
                        layout.heightPercent(100);
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.minWidth(0);
                    }),
                    compactRail(context, theme, Component.translatable("screen.incore.shop.bulk_rows")).layout(layout -> {
                        layout.heightPercent(100);
                        layout.flexBasis(188);
                        layout.minWidth(176);
                        layout.maxWidth(196);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout EXCHANGE = new FixedLayout(8) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            main.addChildren(
                    exchangeBoard(context, theme).layout(layout -> {
                        layout.flexBasisPercent(62);
                        layout.flexGrow(1);
                    }),
                    inlineDetailsDock(context, theme, Component.translatable("screen.incore.shop.transaction_terminal")).layout(layout -> {
                        layout.flexBasisPercent(30);
                        layout.flexGrow(1);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout BOUTIQUE = new FixedLayout(5) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            main.addChildren(
                    boutiqueBoard(context, theme).layout(layout -> {
                        layout.flexBasisPercent(58);
                        layout.flexGrow(1);
                    }),
                    inlineDetailsDock(context, theme, Component.translatable("screen.incore.shop.dossier_panel")).layout(layout -> {
                        layout.flexBasisPercent(30);
                        layout.flexGrow(1);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout ARCADE = new FixedLayout(8) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            main.addChildren(
                    arcadeBoard(context, theme).layout(layout -> {
                        layout.flexBasisPercent(70);
                        layout.flexGrow(1);
                    }),
                    selectionSummary(context, theme).layout(layout -> {
                        layout.flexBasisPercent(22);
                        layout.flexGrow(1);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout ARCHIVE = new FixedLayout(6) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            main.addChildren(
                    sidebar(context, theme).layout(layout -> {
                        layout.flexBasis(144);
                        layout.minWidth(128);
                        layout.maxWidth(156);
                    }),
                    archiveBoard(context, theme).layout(layout -> {
                        layout.flexBasisPercent(68);
                        layout.flexGrow(1);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout ABYSSAL = new FixedLayout(6) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            main.addChild(abyssalBoard(context, theme));
            return main;
        }
    };

    private ShopAppLayouts() {
    }

    static ShopAppLayout forLayout(ShopLayoutId layoutId) {
        return switch (layoutId) {
            case INDUSTRIAL_MARKET -> INDUSTRIAL;
            case COMMODITY_EXCHANGE -> EXCHANGE;
            case LUXURY_BOUTIQUE -> BOUTIQUE;
            case ARCADE_VENDOR -> ARCADE;
            case ARCHIVE_EDITORIAL -> ARCHIVE;
            case ABYSSAL_TERMINAL -> ABYSSAL;
        };
    }

    private static UIElement industrialCenter(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = fillPanelColumn(theme);
        column.addChildren(
                showcaseWithDetails(context, theme, Component.translatable("screen.incore.shop.featured_asset")),
                boardSection(
                        context,
                        theme,
                        Component.translatable("screen.incore.shop.bulk_rows"),
                        industrialBoardOffers(context),
                        true,
                        true
                ),
                ShopAppUiSupport.spacer()
        );
        return column;
    }

    private static UIElement exchangeBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.exchange_board"),
                        Component.translatable("screen.incore.shop.exchange_board_subtitle"),
                        theme
                ),
                navigationStrip(context, theme, 0),
                denseRows(context, theme, context.state().visibleOffers(context.data()), true),
                pager(context, theme)
        );
        return column;
    }

    private static UIElement boutiqueBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.boutique_heading"),
                        Component.translatable("screen.incore.shop.boutique_subtitle"),
                        theme
                ),
                navigationStrip(context, theme, 1),
                heroPanel(context, theme, context.state().showcaseOffer(context.data()), Component.translatable("screen.incore.shop.curated_showcase")),
                boardSection(context, theme, Component.translatable("screen.incore.shop.curated_stack"), context.state().visibleOffers(context.data()), false, false),
                pager(context, theme)
        );
        return column;
    }

    private static UIElement arcadeBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                navigationStrip(context, theme, 2),
                heroPanel(context, theme, context.state().showcaseOffer(context.data()), Component.translatable("screen.incore.shop.promo_banner")),
                boardSection(context, theme, Component.translatable("screen.incore.shop.card_board"), context.state().visibleOffers(context.data()), true, false),
                pager(context, theme)
        );
        return column;
    }

    private static UIElement archiveBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                heroPanel(context, theme, context.state().showcaseOffer(context.data()), Component.translatable("screen.incore.shop.editorial_hero")),
                masonryBoard(context, theme, context.state().visibleOffers(context.data())),
                pager(context, theme)
        );
        return column;
    }

    private static UIElement abyssalBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                navigationStrip(context, theme, 3),
                new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(8);
                }).addChildren(
                        heroPanel(context, theme, context.state().showcaseOffer(context.data()), Component.translatable("screen.incore.shop.terminal_module")).layout(layout -> {
                            layout.flexBasisPercent(44);
                            layout.flexGrow(1);
                        }),
                        inlineDetailsDock(context, theme, Component.translatable("screen.incore.shop.signal_dock")).layout(layout -> {
                            layout.flexBasisPercent(36);
                            layout.flexGrow(1);
                        })
                ),
                boardSection(context, theme, Component.translatable("screen.incore.shop.support_board"), context.state().visibleOffers(context.data()), true, true),
                pager(context, theme)
        );
        return column;
    }

    private static UIElement sidebar(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement sidebar = fillPanelColumn(theme, theme.railFill());
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        sidebar.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.categories_heading"),
                        Component.literal(category == null ? "" : ShopAppUiSupport.availableCurrencyLabel(category.currency())),
                        theme
                ),
                categoryButtons(context, theme, true)
        );
        return sidebar;
    }

    private static UIElement navigationStrip(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme, int style) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(style == 2 ? 4 : 6);
            layout.flexWrap(dev.vfyjxf.taffy.style.FlexWrap.WRAP);
        });
        for (ShopService.CategoryView category : ShopAppUiSupport.categoriesForTab(context.data(), context.state().activeTab())) {
            boolean active = category.categoryId().equals(context.state().selectedCategoryId());
            Button button = switch (style) {
                case 1 -> accentChip(Component.literal(category.displayName()), theme, active);
                case 2 -> loudChip(Component.literal(category.displayName()), theme, active);
                case 3 -> moduleChip(Component.literal(category.displayName()), theme, active);
                default -> ShopAppUiSupport.chipButton(Component.literal(category.displayName()), theme, active);
            };
            button.setOnClick(event -> {
                context.state().selectCategory(category.categoryId(), context.data());
                context.rebuild();
            });
            row.addChild(button);
        }
        return row;
    }

    private static UIElement compactRail(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme, Component title) {
        UIElement rail = fillPanelColumn(theme, theme.railFill());
        rail.addChildren(
                sectionHeader(title, Component.translatable("screen.incore.shop.remaining_feed"), theme),
                denseRows(context, theme, industrialRailOffers(context), true),
                ShopAppUiSupport.spacer(),
                pager(context, theme)
        );
        return rail;
    }

    private static UIElement inlineDetailsDock(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme, Component title) {
        var offer = context.state().selectedOffer(context.data());
        if (offer == null) {
            return ShopAppDetailsView.createEmpty(
                    theme,
                    title,
                    Component.translatable("screen.incore.shop.no_offer_selected")
            );
        }
        UIElement dock = panelColumn(theme, theme.accentSoftFill());
        dock.addChildren(
                sectionHeader(title, Component.literal(offer.displayName()), theme),
                ShopAppDetailsView.create(context, theme, offer, false)
        );
        return dock;
    }

    private static UIElement selectionSummary(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        var offer = context.state().selectedOffer(context.data());
        UIElement column = panelColumn(theme, theme.accentSoftFill());
        column.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.selection_rail"),
                        Component.translatable("screen.incore.shop.selection_rail_subtitle"),
                        theme
                )
        );
        if (offer == null) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offer_selected"), theme.secondaryText()));
            return column;
        }
        column.addChildren(
                heroPanel(context, theme, offer, Component.translatable("screen.incore.shop.selected_offer")),
                metricTile(Component.translatable("screen.incore.shop.price_each"), Component.literal(ShopAppUiSupport.currencyAmountLabel(offer.currency())), theme),
                metricTile(Component.translatable("screen.incore.shop.stock_label"), Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme)
        );
        return column;
    }

    private static UIElement showcaseWithDetails(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme, Component title) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
            layout.minHeight(212);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
        });
        row.addChildren(
                heroPanel(context, theme, context.state().showcaseOffer(context.data()), title).layout(layout -> {
                    layout.flexBasis(0);
                    layout.flexGrow(5);
                    layout.minWidth(0);
                    layout.heightPercent(100);
                }),
                inlineDetailsDock(context, theme, Component.translatable("screen.incore.shop.details_dock")).layout(layout -> {
                    layout.flexBasis(210);
                    layout.flexGrow(2);
                    layout.minWidth(196);
                    layout.maxWidth(236);
                    layout.minHeight(212);
                })
        );
        return row;
    }

    private static UIElement heroPanel(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            @Nullable ShopService.OfferView offer,
            Component title
    ) {
        Button hero = new Button().setText(Component.empty());
        hero.text.setDisplay(false);
        hero.layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.STRETCH);
            layout.justifyContent(AlignContent.FLEX_START);
            layout.gapAll(8);
            layout.paddingAll(8);
        });
        hero.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.softTexture(theme.cardFill()))
                .hoverTexture(ShopAppUiSupport.softTexture(theme.cardHoverFill()))
                .pressedTexture(ShopAppUiSupport.softTexture(theme.cardSelectedFill()))
        );
        if (offer == null) {
            hero.addChildren(
                    sectionHeader(title, Component.translatable("screen.incore.shop.no_offer_selected"), theme),
                    ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.workspace.empty"), theme.secondaryText())
            );
            return hero;
        }
        hero.addChildren(
                sectionHeader(title, Component.literal(offer.displayName()), theme),
                offerPreviewHero(offer, theme),
                offerMetaLine(offer, theme)
        );
        hero.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return hero;
    }

    private static UIElement offerPreviewHero(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.FLEX_START);
            layout.gapAll(10);
        });
        UIElement media = ShopAppUiSupport.mutedSurface(theme, 10);
        media.layout(layout -> {
            layout.width(88);
            layout.height(88);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 52, Component.literal(offer.displayName())));
        row.addChildren(
                media,
                new UIElement().layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(3);
                }).addChildren(
                        textLabel(Component.literal(offer.displayName()), theme.primaryText(), false),
                        ShopAppUiSupport.bodyLabel(Component.literal(ShopAppUiSupport.rewardSummary(offer)), theme.secondaryText()),
                        metricTile(Component.translatable("screen.incore.shop.price_each"), Component.literal(ShopAppUiSupport.currencyAmountLabel(offer.currency())), theme)
                )
        );
        return row;
    }

    private static UIElement offerMetaLine(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(6);
        });
        row.addChildren(
                metricTile(Component.translatable("screen.incore.shop.stock_label"), Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme),
                metricTile(
                        Component.translatable("screen.incore.shop.balance_label"),
                        Component.literal(ShopAppUiSupport.availableCurrencyLabel(offer.currency())),
                        theme
                )
        );
        return row;
    }

    private static List<ShopService.OfferView> industrialRailOffers(ShopAppLayoutContext context) {
        ShopService.TabFeedView feed = ShopAppUiSupport.activeFeed(context.data(), context.state());
        return ShopAppUiSupport.visibleOffers(feed, context.state().offerScrollRow(), INDUSTRIAL_RAIL_ROWS);
    }

    private static List<ShopService.OfferView> industrialBoardOffers(ShopAppLayoutContext context) {
        ShopService.TabFeedView feed = ShopAppUiSupport.activeFeed(context.data(), context.state());
        return ShopAppUiSupport.visibleOffers(
                feed,
                context.state().offerScrollRow() + INDUSTRIAL_RAIL_ROWS,
                INDUSTRIAL_BOARD_ROWS
        );
    }

    private static UIElement boardSection(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            Component title,
            List<ShopService.OfferView> offers,
            boolean compact,
            boolean twoColumns
    ) {
        UIElement column = panelColumn(theme);
        column.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(0);
        });
        column.addChildren(
                sectionHeader(title, Component.literal(Integer.toString(offers.size())), theme),
                twoColumns ? splitBoard(context, theme, offers, compact) : denseRows(context, theme, offers, compact)
        );
        return column;
    }

    private static UIElement masonryBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme, List<ShopService.OfferView> offers) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(8);
        });
        UIElement left = panelColumn(theme);
        UIElement right = panelColumn(theme);
        for (int i = 0; i < offers.size(); i++) {
            (i % 2 == 0 ? left : right).addChild(offerCard(context, offers.get(i), theme, i % 3 == 0 ? 54 : 42, true));
        }
        row.addChildren(left.layout(layout -> layout.flex(1)), right.layout(layout -> layout.flex(1)));
        return row;
    }

    private static UIElement splitBoard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            List<ShopService.OfferView> offers,
            boolean compact
    ) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(8);
        });
        UIElement left = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        UIElement right = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        for (int i = 0; i < offers.size(); i++) {
            (i % 2 == 0 ? left : right).addChild(offerCard(context, offers.get(i), theme, compact ? 34 : 42, compact));
        }
        row.addChildren(left, right);
        return row;
    }

    private static UIElement denseRows(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            List<ShopService.OfferView> offers,
            boolean compact
    ) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        if (offers.isEmpty()) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return column;
        }
        for (ShopService.OfferView offer : offers) {
            column.addChild(offerCard(context, offer, theme, compact ? 32 : 40, compact));
        }
        return column;
    }

    private static UIElement offerCard(
            ShopAppLayoutContext context,
            ShopService.OfferView offer,
            ShopAppUiSupport.TabTheme theme,
            int height,
            boolean compact
    ) {
        boolean selected = offer.offerId().equals(context.state().selectedOfferId());
        Button card = new Button().setText(Component.empty());
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
            layout.paddingAll(6);
        });
        card.text.setDisplay(false);
        ShopAppUiSupport.styleSelectable(card, theme, selected);
        card.addChildren(
                ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), compact ? 16 : 18, Component.literal(offer.displayName())),
                offerText(offer, theme).layout(layout -> layout.flex(1)),
                offerBadge(offer, theme, compact)
        );
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static UIElement offerText(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(1);
        });
        column.addChildren(
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                textLabel(
                        Component.literal(
                                offer.rotationRemainingMillis() >= 0L
                                        ? ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())
                                        : ShopAppUiSupport.rewardSummary(offer)
                        ),
                        offer.locked() ? theme.alertText() : theme.secondaryText(),
                        true
                )
        );
        return column;
    }

    private static UIElement offerBadge(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme, boolean compact) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(compact ? 54 : 104);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.gapAll(1);
        });
        column.addChild(textLabel(Component.literal(ShopAppUiSupport.currencyAmountLabel(offer.currency())), theme.priceText(), true));
        if (!compact) {
            column.addChild(textLabel(Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme.secondaryText(), true));
        }
        return column;
    }

    private static UIElement pager(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
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

        ShopService.TabFeedView feed = ShopAppUiSupport.activeFeed(context.data(), context.state());
        int total = feed.remainingOffers().size();
        int current = total == 0 ? 0 : Math.min(total, context.state().offerScrollRow() + 1);
        Label counter = ShopAppUiSupport.heading(Component.literal(current + "/" + Math.max(1, total)), theme.secondaryText());
        counter.layout(layout -> layout.flex(1));
        counter.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));

        row.addChildren(previous, counter, next);
        return row;
    }

    private static UIElement categoryButtons(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme, boolean fullWidth) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });
        for (ShopService.CategoryView category : ShopAppUiSupport.categoriesForTab(context.data(), context.state().activeTab())) {
            boolean active = category.categoryId().equals(context.state().selectedCategoryId());
            Button button = ShopAppUiSupport.chipButton(Component.literal(category.displayName()), theme, active);
            if (fullWidth) {
                button.layout(layout -> layout.widthPercent(100));
            }
            button.setOnClick(event -> {
                context.state().selectCategory(category.categoryId(), context.data());
                context.rebuild();
            });
            column.addChild(button);
        }
        return column;
    }

    private static Button accentChip(Component text, ShopAppUiSupport.TabTheme theme, boolean active) {
        Button button = ShopAppUiSupport.chipButton(text, theme, active);
        button.layout(layout -> layout.minWidth(120));
        return button;
    }

    private static Button loudChip(Component text, ShopAppUiSupport.TabTheme theme, boolean active) {
        Button button = ShopAppUiSupport.chipButton(text, theme, active);
        button.layout(layout -> {
            layout.minWidth(92);
            layout.height(22);
        });
        return button;
    }

    private static Button moduleChip(Component text, ShopAppUiSupport.TabTheme theme, boolean active) {
        Button button = ShopAppUiSupport.chipButton(text, theme, active);
        button.layout(layout -> {
            layout.minWidth(126);
            layout.height(22);
        });
        return button;
    }

    private static UIElement metricTile(Component title, Component value, ShopAppUiSupport.TabTheme theme) {
        UIElement tile = ShopAppUiSupport.mutedSurface(theme, 5);
        tile.layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
        });
        tile.addChildren(
                textLabel(title, theme.secondaryText(), true),
                textLabel(value, theme.primaryText(), true)
        );
        return tile;
    }

    private static UIElement sectionHeader(Component title, Component subtitle, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(1);
        });
        row.addChildren(
                textLabel(title, theme.primaryText(), true),
                textLabel(subtitle, theme.secondaryText(), true)
        );
        return row;
    }

    private static Label textLabel(Component text, int color, boolean singleLine) {
        Label label = ShopAppUiSupport.heading(text, color);
        if (singleLine) {
            label.textStyle(style -> style.adaptiveWidth(false).textWrap(TextWrap.HIDE).textAlignHorizontal(Horizontal.LEFT));
        }
        return label;
    }

    private static UIElement panelColumn(ShopAppUiSupport.TabTheme theme) {
        return panelColumn(theme, theme.sectionFill());
    }

    private static UIElement panelColumn(ShopAppUiSupport.TabTheme theme, int fill) {
        UIElement column = ShopAppUiSupport.tintedSurface(theme, fill, 8, false);
        column.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        return column;
    }

    private static UIElement fillPanelColumn(ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.layout(layout -> layout.heightPercent(100));
        return column;
    }

    private static UIElement fillPanelColumn(ShopAppUiSupport.TabTheme theme, int fill) {
        UIElement column = panelColumn(theme, fill);
        column.layout(layout -> layout.heightPercent(100));
        return column;
    }

    private static UIElement baseRow() {
        return new UIElement().layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(8);
        });
    }

    private abstract static class FixedLayout implements ShopAppLayout {
        private final int visibleOfferRows;

        private FixedLayout(int visibleOfferRows) {
            this.visibleOfferRows = visibleOfferRows;
        }

        @Override
        public int visibleOfferRows() {
            return visibleOfferRows;
        }
    }
}
