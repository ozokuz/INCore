package ozokuz.incore.integration.ldlib.ui.player;

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
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopDetailsPresentationMode;
import ozokuz.incore.features.shop.ShopLayoutId;
import ozokuz.incore.features.shop.ShopService;

final class ShopAppLayouts {
    private static final int INDUSTRIAL_RAIL_ROWS = 3;
    private static final int INDUSTRIAL_BOARD_ROWS = 2;

    private static final ShopAppLayout INDUSTRIAL = new FixedLayout(INDUSTRIAL_RAIL_ROWS + INDUSTRIAL_BOARD_ROWS) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            ShopService.CategoryView lockedCategory = ShopAppUiSupport.lockedActiveCategory(context.data(), context.state());
            if (lockedCategory != null) {
                main.addChildren(
                        industrialSidebar(context, theme).layout(layout -> {
                            layout.heightPercent(100);
                            layout.flexBasis(168);
                            layout.minWidth(152);
                            layout.maxWidth(184);
                        }),
                        selectedCategoryLockedDisplay(theme, lockedCategory).layout(layout -> {
                            layout.heightPercent(100);
                            layout.flexBasis(0);
                            layout.flexGrow(1);
                            layout.minWidth(0);
                        })
                );
                return main;
            }
            main.addChildren(
                    industrialSidebar(context, theme).layout(layout -> {
                        layout.heightPercent(100);
                        layout.flexBasis(168);
                        layout.minWidth(152);
                        layout.maxWidth(184);
                    }),
                    industrialCenter(context, theme).layout(layout -> {
                        layout.heightPercent(100);
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.minWidth(0);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout EXCHANGE = new FixedLayout(1) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            ShopService.CategoryView lockedCategory = ShopAppUiSupport.lockedActiveCategory(context.data(), context.state());
            if (lockedCategory != null) {
                main.addChildren(
                        commodityRail(context, theme).layout(layout -> {
                            layout.flexBasis(136);
                            layout.flexGrow(0);
                            layout.flexShrink(1);
                            layout.minWidth(112);
                            layout.maxWidth(152);
                        }),
                        selectedCategoryLockedDisplay(theme, lockedCategory).layout(layout -> {
                            layout.flexBasis(0);
                            layout.flexGrow(1);
                            layout.flexShrink(1);
                            layout.minWidth(0);
                        })
                );
                return main;
            }
            main.addChildren(
                    commodityRail(context, theme).layout(layout -> {
                        layout.flexBasis(136);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.minWidth(112);
                        layout.maxWidth(152);
                    }),
                    exchangeBoard(context, theme).layout(layout -> {
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.flexShrink(1);
                        layout.minWidth(0);
                    }),
                    commodityDetailsPane(context, theme).layout(layout -> {
                        layout.flexBasis(244);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.minWidth(188);
                        layout.maxWidth(244);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout BOUTIQUE = new FixedLayout(6) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            main.addChildren(
                    ShopAppUiSupport.spacer().layout(layout -> {
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.minWidth(0);
                    }),
                    boutiqueBoard(context, theme).layout(layout -> {
                        layout.flexBasis(480);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.maxWidth(540);
                        layout.minWidth(420);
                    }),
                    ShopAppUiSupport.spacer().layout(layout -> {
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.minWidth(0);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout ARCADE = new FixedLayout(8) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            ShopService.CategoryView lockedCategory = ShopAppUiSupport.lockedActiveCategory(context.data(), context.state());
            if (lockedCategory != null) {
                main.addChildren(
                        arcadeCategoryRail(context, theme).layout(layout -> {
                            layout.flexBasis(170);
                            layout.flexGrow(0);
                            layout.flexShrink(1);
                            layout.minWidth(152);
                            layout.maxWidth(188);
                        }),
                        selectedCategoryLockedDisplay(theme, lockedCategory).layout(layout -> {
                            layout.flexBasis(0);
                            layout.flexGrow(1);
                            layout.minWidth(0);
                        })
                );
                return main;
            }
            main.addChildren(
                    arcadeCategoryRail(context, theme).layout(layout -> {
                        layout.flexBasis(170);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.minWidth(152);
                        layout.maxWidth(188);
                    }),
                    arcadeBoard(context, theme).layout(layout -> {
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.minWidth(0);
                    }),
                    arcadePurchaseDock(context, theme).layout(layout -> {
                        layout.flexBasis(246);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.minWidth(214);
                        layout.maxWidth(270);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout ARCHIVE = new FixedLayout(6) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            ShopService.CategoryView lockedCategory = ShopAppUiSupport.lockedActiveCategory(context.data(), context.state());
            if (lockedCategory != null) {
                main.addChildren(
                        archiveCategoryRail(context, theme).layout(layout -> {
                            layout.flexBasis(170);
                            layout.flexGrow(0);
                            layout.flexShrink(1);
                            layout.minWidth(152);
                            layout.maxWidth(188);
                        }),
                        selectedCategoryLockedDisplay(theme, lockedCategory).layout(layout -> {
                            layout.flexBasis(0);
                            layout.flexGrow(1);
                            layout.minWidth(0);
                        })
                );
                return main;
            }
            main.addChildren(
                    archiveCategoryRail(context, theme).layout(layout -> {
                        layout.flexBasis(170);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.minWidth(152);
                        layout.maxWidth(188);
                    }),
                    archiveBoard(context, theme).layout(layout -> {
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.minWidth(0);
                    }),
                    archiveIntelRail(context, theme).layout(layout -> {
                        layout.flexBasis(246);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.minWidth(220);
                        layout.maxWidth(272);
                    })
            );
            return main;
        }
    };

    private static final ShopAppLayout ABYSSAL = new FixedLayout(6) {
        @Override
        public UIElement createContentRow(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
            UIElement main = baseRow();
            ShopService.CategoryView lockedCategory = ShopAppUiSupport.lockedActiveCategory(context.data(), context.state());
            if (lockedCategory != null) {
                main.addChildren(
                        abyssalCategoryRail(context, theme).layout(layout -> {
                            layout.flexBasis(170);
                            layout.flexGrow(0);
                            layout.flexShrink(1);
                            layout.minWidth(152);
                            layout.maxWidth(188);
                        }),
                        selectedCategoryLockedDisplay(theme, lockedCategory).layout(layout -> {
                            layout.flexBasis(0);
                            layout.flexGrow(1);
                            layout.minWidth(0);
                        })
                );
                return main;
            }
            main.addChildren(
                    abyssalCategoryRail(context, theme).layout(layout -> {
                        layout.flexBasis(170);
                        layout.flexGrow(0);
                        layout.flexShrink(1);
                        layout.minWidth(152);
                        layout.maxWidth(188);
                    }),
                    abyssalBoard(context, theme).layout(layout -> {
                        layout.flexBasis(0);
                        layout.flexGrow(1);
                        layout.minWidth(0);
                    })
            );
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
        column.layout(layout -> layout.minWidth(0));
        column.addChildren(
                industrialListingScroller(context, theme)
        );
        return column;
    }

    private static UIElement industrialSidebar(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement sidebar = fillPanelColumn(theme, theme.railFill());
        sidebar.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.categories_heading"),
                        theme
                ),
                categoryScroller(context, theme)
        );
        return sidebar;
    }

    private static UIElement industrialListingScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ScrollerView scroller = verticalScroller(context, theme, "content", 8);
        scroller.addScrollViewChildren(
                industrialTopShelf(context, theme),
                boardSection(
                        context,
                        theme,
                        Component.translatable("screen.incore.shop.offers_heading"),
                        industrialRemainingOffers(context),
                        false,
                        true
                )
        );
        return scroller;
    }

    private static UIElement industrialTopShelf(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
        });
        row.addChildren(
                industrialFeaturePanel(context, theme).layout(layout -> {
                    layout.flexBasis(0);
                    layout.flexGrow(3);
                    layout.minWidth(0);
                    layout.heightPercent(100);
                }),
                industrialHighlightRail(context, theme).layout(layout -> {
                    layout.flexBasis(208);
                    layout.flexGrow(1);
                    layout.minWidth(188);
                    layout.maxWidth(224);
                    layout.heightPercent(100);
                })
        );
        return new UIElement().layout(layout -> {
            layout.widthPercent(100).minWidth(0).flexDirection(FlexDirection.COLUMN).alignItems(AlignItems.STRETCH).gapAll(8);
        }).addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.featured_asset"),
                        theme
                ),
                row
        );
    }

    private static UIElement industrialFeaturePanel(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ShopService.OfferView slotOffer = industrialSlotOffer(context);
        if (slotOffer == null) {
            return ShopAppDetailsView.createEmpty(
                    theme,
                    Component.translatable("screen.incore.shop.featured_asset"),
                    Component.translatable("screen.incore.shop.no_offer_selected")
            );
        }

        Button panel = new Button().setText(Component.empty());
        panel.text.setDisplay(false);
        panel.layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
            layout.paddingAll(8);
            layout.justifyContent(AlignContent.FLEX_START);
        });
        panel.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.accentSoftFill(), theme.divider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );
        UIElement content = new UIElement().setAllowHitTest(false);
        content.layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(6);
        });
        Label titleLabel = textLabel(Component.literal(slotOffer.displayName()), theme.primaryText(), true);
        titleLabel.layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
        });
        row.addChild(titleLabel);
        Label summaryLabel = optionalHeaderMetaLabel(slotOffer, theme);
        if (summaryLabel != null) {
            row.addChild(summaryLabel);
        }
        content.addChildren(
                row,
                new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.minWidth(0);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.alignItems(AlignItems.STRETCH);
                    layout.gapAll(8);
                }).addChildren(
                        industrialHeroMedia(slotOffer, theme),
                        new UIElement().layout(layout -> {
                            layout.flexBasis(0);
                            layout.flexGrow(1);
                            layout.minWidth(0);
                            layout.flexDirection(FlexDirection.COLUMN);
                            layout.gapAll(4);
                        }).addChildren(
                                industrialStatsBox(slotOffer, theme),
                                industrialEditorialShowcase(context, theme, slotOffer)
                        )
                )
        );
        panel.setOnClick(event -> {
            context.state().openDetails(slotOffer.offerId(), context.data());
            context.rebuild();
        });
        panel.addChild(content);
        return panel;
    }

    private static UIElement industrialHeroMedia(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement media = ShopAppUiSupport.tintedSurface(theme, theme.cardFill(), 10, true);
        media.layout(layout -> {
            layout.width(148);
            layout.minHeight(148);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 72, Component.literal(offer.displayName())));
        return media;
    }

    private static UIElement industrialHighlightRail(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = fillPanelColumn(theme, theme.railFill());
        column.addChildren(
                sectionHeader(
                        Component.translatable("screen.incore.shop.remaining_feed"),
                        theme
                ),
                industrialHighlightScroller(context, theme)
        );
        return column;
    }

    private static UIElement industrialHighlightScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        List<ShopService.OfferView> offers = industrialHighlightOffers(context);
        if (offers.isEmpty()) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return column;
        }
        for (ShopService.OfferView offer : offers) {
            column.addChild(industrialHighlightCard(context, theme, offer));
        }
        return column;
    }

    private static UIElement industrialHighlightCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        return offerCard(context, offer, theme, 36, true);
    }

    private static UIElement categoryScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ScrollerView scroller = verticalScroller(context, theme, "categories", 4);
        scroller.addScrollViewChild(categoryButtons(context, theme, true));
        return scroller;
    }

    private static ScrollerView verticalScroller(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            String keySuffix,
            int gap
    ) {
        ScrollerView scroller = new ScrollerView()
                .scrollerStyle(style -> style
                        .mode(ScrollerMode.VERTICAL)
                        .horizontalScrollDisplay(ScrollDisplay.NEVER)
                        .verticalScrollDisplay(ScrollDisplay.AUTO)
                        .minScrollPixel(0.0F)
                        .maxScrollPixel(18.0F)
                );
        scroller.layout(layout -> {
            layout.flex(1);
            layout.widthPercent(100);
            layout.minHeight(0);
        });
        scroller.viewPort
                .style(style -> style.backgroundTexture(ShopAppUiSupport.flatTexture(0x00000000)))
                .layout(layout -> {
                    layout.flex(1);
                    layout.minHeight(0);
                    layout.paddingAll(0);
                });
        scroller.viewContainer.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(gap);
        });
        scroller.verticalContainer.layout(layout -> layout.gapColumn(4));
        scroller.verticalScroller.layout(layout -> layout.width(6));
        scroller.verticalScroller.headButton.setDisplay(false);
        scroller.verticalScroller.tailButton.setDisplay(false);
        scroller.horizontalScroller.headButton.setDisplay(false);
        scroller.horizontalScroller.tailButton.setDisplay(false);
        scroller.verticalScroller.scrollContainer.style(style -> style.backgroundTexture(ShopAppUiSupport.softTexture(theme.insetFill())));
        scroller.verticalScroller.scrollBar.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.accentSoftFill(), theme.accentDivider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.buttonPressedFill(), theme.accent()))
        );
        String scrollKey = context.state().activeTab().serialized() + "." + keySuffix;
        scroller.verticalScroller.setNormalizedValue(context.state().scrollerPosition(scrollKey), false);
        scroller.verticalScroller.setOnValueChanged(value -> context.state().setScrollerPosition(scrollKey, scroller.verticalScroller.getNormalizedValue()));
        return scroller;
    }

    private static UIElement exchangeBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChild(commodityListingScroller(context, theme));
        return column;
    }

    private static UIElement commodityRail(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement rail = fillPanelColumn(theme, theme.railFill());
        rail.addChild(categoryScroller(context, theme));
        return rail;
    }

    private static UIElement commodityListingScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ScrollerView scroller = verticalScroller(context, theme, "offers", 8);
        scroller.addScrollViewChild(commodityOfferColumn(context, theme));
        return scroller;
    }

    private static UIElement commodityDetailsPane(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ShopService.OfferView offer = context.state().selectedOffer(context.data());
        if (offer == null) {
            return ShopAppDetailsView.createEmpty(
                    theme,
                    Component.translatable("screen.incore.shop.transaction_terminal"),
                    Component.translatable("screen.incore.shop.no_offer_selected")
            ).layout(layout -> layout.heightPercent(100));
        }
        return ShopAppDetailsView.create(context, theme, offer, false).layout(layout -> layout.heightPercent(100));
    }

    private static UIElement commodityOfferColumn(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        List<ShopService.OfferView> offers = ShopAppUiSupport.displayOffers(ShopAppUiSupport.activeFeed(context.data(), context.state()));
        if (offers.isEmpty()) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return column;
        }
        for (ShopService.OfferView offer : offers) {
            column.addChild(commodityOfferCard(context, theme, offer));
        }
        return column;
    }

    private static UIElement commodityOfferCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        boolean selected = offer.offerId().equals(context.state().selectedOfferId());
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(54);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(8);
            layout.paddingAll(6);
        });
        card.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(selected ? theme.cardSelectedFill() : theme.cardFill(), selected ? theme.accentDivider() : theme.divider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );
        UIElement marker = new UIElement();
        marker.style(style -> style.backgroundTexture(ShopAppUiSupport.flatTexture(selected ? theme.accent() : theme.divider())));
        marker.layout(layout -> {
            layout.width(3);
            layout.heightPercent(100);
        });
        UIElement iconFrame = ShopAppUiSupport.tintedSurface(theme, selected ? theme.accentSoftFill() : theme.insetFill(), 6, false);
        iconFrame.layout(layout -> {
            layout.width(42);
            layout.height(42);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        iconFrame.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 22, Component.literal(offer.displayName())));
        UIElement textBlock = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
        });
        textBlock.addChildren(
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                textLabel(commodityDescriptor(offer), theme.secondaryText(), true)
        );
        UIElement priceBlock = new UIElement().layout(layout -> {
            layout.width(120);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.justifyContent(AlignContent.CENTER);
            layout.gapAll(2);
        });
        priceBlock.addChildren(
                ShopAppUiSupport.currencyValue(
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme.priceText()
                ),
                valueLabel(Component.translatable("screen.incore.shop.stock.remaining", ShopAppUiSupport.stockLabel(offer.availableStock())), theme.secondaryText())
        );
        card.addChildren(marker, iconFrame, textBlock, priceBlock);
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static Component commodityDescriptor(ShopService.OfferView offer) {
        if (offer.rotationRemainingMillis() >= 0L) {
            return Component.translatable("screen.incore.shop.time_left_format", ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis()));
        }
        if (offer.rewardEntries().size() > 1) {
            return Component.literal(ShopAppUiSupport.rewardSummary(offer));
        }
        if (!offer.rewardEntries().isEmpty()) {
            ShopService.RewardEntryView entry = offer.rewardEntries().getFirst();
            return Component.translatable(
                    "screen.incore.shop.commodity_reward_line",
                    ShopAppUiSupport.rewardEntryLabel(entry),
                    entry.count()
            );
        }
        return Component.translatable("screen.incore.shop.stock.remaining", ShopAppUiSupport.stockLabel(offer.availableStock()));
    }


    private static UIElement boutiqueBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.layout(layout -> {
            layout.heightPercent(100);
            layout.maxWidth(540);
        });
        column.addChildren(
                boutiqueScroller(context, theme)
        );
        return column;
    }

    private static UIElement boutiqueScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ScrollerView scroller = verticalScroller(context, theme, "content", 12);
        scroller.addScrollViewChild(boutiqueCategoryGroups(context, theme));
        return scroller;
    }

    private static UIElement boutiqueCategoryGroups(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(14);
        });
        List<ShopService.CategoryView> categories = ShopAppUiSupport.categoriesForTab(context.data(), context.state().activeTab());
        if (categories.isEmpty()) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_categories"), theme.secondaryText()));
            return column;
        }
        for (int i = 0; i < categories.size(); i++) {
            column.addChild(boutiqueCategoryGroup(context, theme, categories.get(i), i == 0));
        }
        return column;
    }

    private static UIElement boutiqueCategoryGroup(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.CategoryView category,
            boolean includeHero
    ) {
        if (category.locked()) {
            return selectedCategoryLockedDisplay(theme, category);
        }
        ShopService.TabFeedView feed = ShopAppUiSupport.feedForTab(context.data(), context.state().activeTab(), category.categoryId());
        List<ShopService.OfferView> displayOffers = ShopAppUiSupport.displayOffers(feed);
        UIElement group = ShopAppUiSupport.tintedSurface(theme, theme.cardFill(), 10, false);
        group.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(10);
            layout.paddingAll(8);
        });
        group.addChild(dualSectionHeader(
                Component.literal(category.displayName()),
                boutiqueCategorySubtitle(category, displayOffers),
                theme
        ));

        if (displayOffers.isEmpty()) {
            group.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return group;
        }

        ShopService.OfferView heroOffer = includeHero ? boutiqueHeroOffer(feed, displayOffers) : null;
        if (heroOffer != null) {
            group.addChild(boutiquePrimaryHeroCard(context, theme, category, heroOffer));
        }

        List<ShopService.OfferView> secondaryOffers = boutiqueSecondaryOffers(displayOffers, heroOffer);
        if (!secondaryOffers.isEmpty()) {
            group.addChild(boutiqueSecondaryCardGrid(context, theme, secondaryOffers));
        }

        return group;
    }

    private static Component boutiqueCategorySubtitle(ShopService.CategoryView category, List<ShopService.OfferView> offers) {
        String currencyLabel = category.currency().label() == null ? "" : category.currency().label();
        String availability = ShopAppUiSupport.availableCurrencyAmount(category.currency()) + (currencyLabel.isBlank() ? "" : " " + currencyLabel);
        return Component.literal(offers.size() + " offers • " + availability);
    }

    private static @Nullable ShopService.OfferView boutiqueHeroOffer(
            ShopService.TabFeedView feed,
            List<ShopService.OfferView> displayOffers
    ) {
        if (!feed.showcaseOffers().isEmpty()) {
            return feed.showcaseOffers().getFirst();
        }
        return displayOffers.isEmpty() ? null : displayOffers.getFirst();
    }

    private static List<ShopService.OfferView> boutiqueSecondaryOffers(
            List<ShopService.OfferView> displayOffers,
            @Nullable ShopService.OfferView heroOffer
    ) {
        if (heroOffer == null) {
            return List.copyOf(displayOffers);
        }
        List<ShopService.OfferView> offers = new ArrayList<>();
        for (ShopService.OfferView offer : displayOffers) {
            if (offer.offerId().equals(heroOffer.offerId())) {
                continue;
            }
            offers.add(offer);
        }
        return List.copyOf(offers);
    }

    private static UIElement boutiquePrimaryHeroCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.CategoryView category,
            ShopService.OfferView offer
    ) {
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(196);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(10);
            layout.paddingAll(10);
        });
        card.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.accentSoftFill(), theme.divider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );

        UIElement editorial = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(8);
        });
        editorial.addChildren(
                dualSectionHeader(Component.literal(category.displayName()), heroMetaSubtitle(offer), theme),
                boutiqueHeroCopy(offer, theme),
                new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(6);
                }).addChildren(
                        showcaseMetricTile(
                                Component.translatable("screen.incore.shop.price_label"),
                                Component.literal(Integer.toString(ShopAppUiSupport.currencyAmount(offer.currency()))),
                                theme
                        ),
                        showcaseMetricTile(
                                Component.translatable("screen.incore.shop.stock_label"),
                                Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())),
                                theme
                        )
                ),
                ShopAppUiSupport.highlightedSurface(theme, 6).layout(layout -> layout.width(132)).addChild(
                        textLabel(Component.translatable("screen.incore.shop.choose_offer"), theme.primaryText(), true)
                )
        );

        UIElement media = ShopAppUiSupport.tintedSurface(theme, theme.insetFill(), 10, true);
        media.layout(layout -> {
            layout.width(184);
            layout.minHeight(176);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 96, Component.literal(offer.displayName())));

        card.addChildren(editorial, media);
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static Component heroMetaSubtitle(ShopService.OfferView offer) {
        if (offer.rotationRemainingMillis() >= 0L) {
            return Component.translatable("screen.incore.shop.time_left_format", ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis()));
        }
        if (offer.rewardEntries().size() > 1) {
            return Component.literal(ShopAppUiSupport.rewardSummary(offer));
        }
        if (!offer.rewardEntries().isEmpty()) {
            return Component.literal(ShopAppUiSupport.rewardEntryLabel(offer.rewardEntries().getFirst()));
        }
        return Component.translatable("screen.incore.shop.curated");
    }

    private static UIElement boutiqueHeroCopy(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        column.addChildren(
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                textLabel(Component.literal(ShopAppUiSupport.rewardSummary(offer)), theme.secondaryText(), false)
        );
        return column;
    }

    private static UIElement boutiqueSecondaryCardGrid(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            List<ShopService.OfferView> offers
    ) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
        });
        UIElement left = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        UIElement right = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        for (int i = 0; i < offers.size(); i++) {
            (i % 2 == 0 ? left : right).addChild(boutiqueOfferCard(context, theme, offers.get(i), i == 0));
        }
        row.addChildren(left, right);
        return row;
    }

    private static UIElement boutiqueOfferCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer,
            boolean emphasized
    ) {
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(emphasized ? 140 : 112);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
            layout.paddingAll(8);
        });
        card.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.cardFill(), theme.divider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );

        UIElement media = ShopAppUiSupport.tintedSurface(theme, emphasized ? theme.accentSoftFill() : theme.insetFill(), 8, true);
        media.layout(layout -> {
            layout.widthPercent(100);
            layout.height(emphasized ? 68 : 52);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), emphasized ? 34 : 28, Component.literal(offer.displayName())));

        card.addChildren(
                media,
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                textLabel(heroMetaSubtitle(offer), theme.secondaryText(), true),
                new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                    layout.alignItems(AlignItems.CENTER);
                    layout.gapAll(6);
                }).addChildren(
                        ShopAppUiSupport.currencyValue(
                                offer.currency(),
                                ShopAppUiSupport.currencyAmount(offer.currency()),
                                theme.priceText()
                        ),
                        valueLabel(Component.translatable("screen.incore.shop.stock.remaining", ShopAppUiSupport.stockLabel(offer.availableStock())), theme.secondaryText())
                )
        );
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static UIElement arcadeBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                arcadeScroller(context, theme)
        );
        return column;
    }

    private static UIElement archiveBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                archiveScroller(context, theme)
        );
        return column;
    }

    private static UIElement abyssalBoard(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = panelColumn(theme);
        column.addChildren(
                abyssalScroller(context, theme)
        );
        return column;
    }

    private static UIElement arcadeScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ScrollerView scroller = verticalScroller(context, theme, "content", 10);
        scroller.addScrollViewChild(arcadeSections(context, theme));
        return scroller;
    }

    private static UIElement arcadeSections(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(10);
        });
        ShopService.OfferView showcase = context.state().showcaseOffer(context.data());
        if (showcase != null) {
            column.addChild(arcadeHeroPanel(context, theme, showcase));
        }
        List<ShopService.OfferView> offers = ShopAppUiSupport.displayOffers(ShopAppUiSupport.activeFeed(context.data(), context.state()));
        column.addChildren(
                arcadeProductGrid(context, theme, offers),
                arcadeDealBoard(context, theme, offers)
        );
        return column;
    }

    private static UIElement arcadeHeroPanel(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        boolean selected = arcadeSelectedOffer(context, context.data(), offer).offerId().equals(offer.offerId());
        Button panel = new Button().setText(Component.empty());
        panel.text.setDisplay(false);
        panel.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(156);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(10);
            layout.paddingAll(10);
        });
        ShopAppUiSupport.styleSelectable(panel, theme, selected);

        UIElement copy = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(8);
        });
        copy.addChildren(
                dualSectionHeader(Component.translatable("screen.incore.shop.promo_banner"), heroMetaSubtitle(offer), theme),
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                ShopAppUiSupport.bodyLabel(Component.literal(arcadeNarrativeCopy(offer)), theme.secondaryText()),
                ShopAppUiSupport.currencyValue(
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme.priceText()
                )
        );

        UIElement media = ShopAppUiSupport.tintedSurface(theme, theme.insetFill(), 10, true);
        media.layout(layout -> {
            layout.width(120);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 58, Component.literal(offer.displayName())));

        panel.addChildren(copy, media);
        panel.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return panel;
    }

    private static String arcadeNarrativeCopy(ShopService.OfferView offer) {
        if (offer.rotationRemainingMillis() >= 0L) {
            return "Rotation window " + ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())
                    + ". " + ShopAppUiSupport.rewardSummary(offer) + " ready for rapid checkout.";
        }
        return "Priority cache ready for dispatch. " + ShopAppUiSupport.rewardSummary(offer) + " available in this vendor cycle.";
    }

    private static UIElement arcadeProductGrid(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            List<ShopService.OfferView> offers
    ) {
        UIElement section = panelColumn(theme, theme.cardFill());
        section.addChild(dualSectionHeader(
                Component.translatable("screen.incore.shop.card_board"),
                Component.translatable("screen.incore.shop.remaining_feed"),
                theme
        ));
        if (offers.isEmpty()) {
            section.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return section;
        }
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
        });
        UIElement left = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        UIElement right = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        int limit = Math.min(4, offers.size());
        for (int i = 0; i < limit; i++) {
            (i % 2 == 0 ? left : right).addChild(arcadeProductCard(context, theme, offers.get(i)));
        }
        row.addChildren(left, right);
        section.addChild(row);
        return section;
    }

    private static UIElement arcadeProductCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        boolean selected = arcadeSelectedOffer(context, context.data(), offer).offerId().equals(offer.offerId());
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(126);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
            layout.paddingAll(8);
        });
        ShopAppUiSupport.styleSelectable(card, theme, selected);

        UIElement top = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        top.addChildren(
                ShopAppUiSupport.tintedSurface(theme, selected ? theme.accentSoftFill() : theme.insetFill(), 6, true).layout(layout -> {
                    layout.width(44);
                    layout.height(44);
                    layout.alignItems(AlignItems.CENTER);
                    layout.justifyContent(AlignContent.CENTER);
                }).addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 24, Component.literal(offer.displayName()))),
                ShopAppUiSupport.currencyValue(
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme.priceText()
                )
        );
        card.addChildren(
                top,
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                textLabel(heroMetaSubtitle(offer), theme.secondaryText(), true)
        );
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static UIElement arcadeDealBoard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            List<ShopService.OfferView> offers
    ) {
        UIElement board = panelColumn(theme, theme.insetFill());
        board.addChildren(
                dualSectionHeader(
                        Component.translatable("screen.incore.shop.selection_rail"),
                        Component.translatable("screen.incore.shop.selection_rail_subtitle"),
                        theme
                )
        );
        if (offers.isEmpty()) {
            board.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return board;
        }
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(12);
        });
        UIElement left = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        left.addChildren(
                textLabel(Component.translatable("screen.incore.shop.editorial_hero"), theme.primaryText(), true),
                ShopAppUiSupport.bodyLabel(Component.literal("Cycle inventory is unstable. Pick from the active vendor lane before the window rolls."), theme.secondaryText())
        );
        UIElement right = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        int start = Math.min(4, offers.size());
        for (int i = start; i < offers.size(); i++) {
            right.addChild(arcadeDealRow(context, theme, offers.get(i)));
        }
        if (offers.size() <= start) {
            for (int i = 0; i < Math.min(3, offers.size()); i++) {
                right.addChild(arcadeDealRow(context, theme, offers.get(i)));
            }
        }
        row.addChildren(left, right);
        board.addChild(row);
        return board;
    }

    private static UIElement arcadeDealRow(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        boolean selected = arcadeSelectedOffer(context, context.data(), offer).offerId().equals(offer.offerId());
        Button row = new Button().setText(Component.empty());
        row.text.setDisplay(false);
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(34);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(6);
            layout.paddingAll(6);
        });
        ShopAppUiSupport.styleSelectable(row, theme, selected);
        row.addChildren(
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true).layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                }),
                ShopAppUiSupport.currencyValue(
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme.priceText()
                )
        );
        row.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return row;
    }

    private static UIElement archiveScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ScrollerView scroller = verticalScroller(context, theme, "content", 10);
        scroller.addScrollViewChild(archiveSections(context, theme));
        return scroller;
    }

    private static UIElement archiveSections(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(10);
        });
        ShopService.OfferView showcase = archiveFeaturedOffer(context);
        if (showcase != null) {
            column.addChild(archiveHeroCard(context, theme, showcase));
        }
        List<ShopService.OfferView> offers = ShopAppUiSupport.displayOffers(ShopAppUiSupport.activeFeed(context.data(), context.state()));
        if (offers.isEmpty()) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return column;
        }
        for (ShopService.OfferView offer : offers) {
            if (showcase != null && showcase.offerId().equals(offer.offerId())) {
                continue;
            }
            column.addChild(archiveEditorialCard(context, theme, offer));
        }
        return column;
    }

    private static UIElement archiveCategoryRail(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement rail = fillPanelColumn(theme, theme.railFill());
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        rail.addChildren(
                dualSectionHeader(
                        Component.translatable("screen.incore.shop.categories_heading"),
                        category == null ? Component.empty() : Component.literal(category.displayName()),
                        theme
                ),
                category == null
                        ? textLabel(Component.empty(), theme.secondaryText(), true)
                        : ShopAppUiSupport.currencyValue(
                                category.currency(),
                                ShopAppUiSupport.availableCurrencyAmount(category.currency()),
                                theme.priceText()
                        ),
                categoryScroller(context, theme)
        );
        return rail;
    }

    private static UIElement archiveIntelRail(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ShopService.OfferView offer = archiveFeaturedOffer(context);
        UIElement rail = fillPanelColumn(theme, theme.accentSoftFill());
        rail.addChildren(
                dualSectionHeader(
                        Component.translatable("screen.incore.shop.selected_offer"),
                        Component.translatable("screen.incore.shop.selection_rail_subtitle"),
                        theme
                )
        );
        if (offer == null) {
            rail.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offer_selected"), theme.secondaryText()));
            return rail;
        }
        rail.addChildren(
                archiveRailOfferDisplay(context, theme, offer)
        );
        return rail;
    }

    private static ShopService.OfferView archiveFeaturedOffer(ShopAppLayoutContext context) {
        ShopService.OfferView selected = context.state().effectiveSelectedOffer(context.data());
        if (selected != null) {
            return selected;
        }
        return context.state().showcaseOffer(context.data());
    }

    private static UIElement archiveHeroCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(176);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(12);
            layout.paddingAll(10);
        });
        card.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.cardFill(), theme.accentDivider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );
        UIElement copy = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(8);
        });
        copy.addChildren(
                dualSectionHeader(Component.translatable("screen.incore.shop.editorial_hero"), heroMetaSubtitle(offer), theme),
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                ShopAppUiSupport.bodyLabel(Component.literal("Curated artifact briefing assembled from the active archive intake."), theme.secondaryText()),
                new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.gapAll(8);
                }).addChildren(
                        showcaseMetricTile(
                                Component.translatable("screen.incore.shop.price_label"),
                                Component.literal(Integer.toString(ShopAppUiSupport.currencyAmount(offer.currency()))),
                                theme
                        ),
                        showcaseMetricTile(
                                Component.translatable("screen.incore.shop.stock_label"),
                                Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())),
                                theme
                        )
                )
        );
        UIElement media = ShopAppUiSupport.tintedSurface(theme, theme.insetFill(), 10, true);
        media.layout(layout -> {
            layout.width(154);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 72, Component.literal(offer.displayName())));
        card.addChildren(copy, media);
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static UIElement archiveEditorialCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(118);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(10);
            layout.paddingAll(8);
        });
        card.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.sectionFill(), theme.divider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );
        UIElement media = ShopAppUiSupport.tintedSurface(theme, theme.insetFill(), 8, true);
        media.layout(layout -> {
            layout.width(84);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 40, Component.literal(offer.displayName())));
        UIElement copy = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
        });
        copy.addChildren(
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                ShopAppUiSupport.bodyLabel(Component.literal("Field note synchronized. " + heroMetaSubtitle(offer).getString()), theme.secondaryText())
        );
        card.addChildren(
                media,
                copy,
                offerBadge(offer, theme, false)
        );
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static UIElement abyssalScroller(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ScrollerView scroller = verticalScroller(context, theme, "content", 10);
        scroller.addScrollViewChild(abyssalSections(context, theme));
        return scroller;
    }

    private static UIElement abyssalSections(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(10);
        });
        ShopService.OfferView showcase = context.state().showcaseOffer(context.data());
        if (showcase != null) {
            column.addChild(abyssalHeroPanel(context, theme, showcase));
        }
        column.addChild(abyssalModuleBoard(
                context,
                theme,
                ShopAppUiSupport.displayOffers(ShopAppUiSupport.activeFeed(context.data(), context.state()))
        ));
        return column;
    }

    private static UIElement abyssalHeroPanel(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        Button panel = new Button().setText(Component.empty());
        panel.text.setDisplay(false);
        panel.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(174);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(12);
            layout.paddingAll(12);
        });
        panel.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.sectionFill(), theme.accentDivider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );
        UIElement copy = new UIElement().layout(layout -> {
            layout.flexBasis(0);
            layout.flexGrow(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(8);
        });
        copy.addChildren(
                dualSectionHeader(Component.translatable("screen.incore.shop.terminal_module"), heroMetaSubtitle(offer), theme),
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                ShopAppUiSupport.bodyLabel(Component.literal("Priority shipment available. Synchronize the terminal with the highlighted support loadout."), theme.secondaryText()),
                showcaseMetricTile(
                        Component.translatable("screen.incore.shop.price_label"),
                        Component.literal(Integer.toString(ShopAppUiSupport.currencyAmount(offer.currency()))),
                        theme
                )
        );
        UIElement media = ShopAppUiSupport.tintedSurface(theme, theme.insetFill(), 10, true);
        media.layout(layout -> {
            layout.width(168);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 76, Component.literal(offer.displayName())));
        panel.addChildren(copy, media);
        panel.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return panel;
    }

    private static UIElement abyssalModuleBoard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            List<ShopService.OfferView> offers
    ) {
        UIElement board = panelColumn(theme, theme.cardFill());
        board.addChild(dualSectionHeader(
                Component.translatable("screen.incore.shop.support_board"),
                Component.translatable("screen.incore.shop.remaining_feed"),
                theme
        ));
        if (offers.isEmpty()) {
            board.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.no_offers"), theme.secondaryText()));
            return board;
        }
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.gapAll(8);
        });
        UIElement left = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        UIElement right = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(8);
        });
        for (int i = 0; i < offers.size(); i++) {
            (i % 2 == 0 ? left : right).addChild(abyssalModuleCard(context, theme, offers.get(i)));
        }
        row.addChildren(left, right);
        board.addChild(row);
        return board;
    }

    private static UIElement abyssalModuleCard(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(148);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
            layout.paddingAll(8);
        });
        card.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.sectionFill(), theme.divider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );
        card.addChildren(
                abyssalLabelChip(heroMetaSubtitle(offer), theme),
                ShopAppUiSupport.tintedSurface(theme, theme.insetFill(), 8, true).layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(64);
                    layout.alignItems(AlignItems.CENTER);
                    layout.justifyContent(AlignContent.CENTER);
                }).addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 34, Component.literal(offer.displayName()))),
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                new UIElement().layout(layout -> {
                    layout.widthPercent(100);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.SPACE_BETWEEN);
                    layout.alignItems(AlignItems.CENTER);
                    layout.gapAll(6);
                }).addChildren(
                        ShopAppUiSupport.currencyValue(
                                offer.currency(),
                                ShopAppUiSupport.currencyAmount(offer.currency()),
                                theme.priceText()
                        ),
                        valueLabel(Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme.secondaryText())
                )
        );
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static UIElement abyssalLabelChip(Component text, ShopAppUiSupport.TabTheme theme) {
        UIElement chip = ShopAppUiSupport.highlightedSurface(theme, 4);
        chip.layout(layout -> layout.width(126));
        chip.addChild(textLabel(text, theme.primaryText(), true));
        return chip;
    }

    private static UIElement sidebar(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement sidebar = fillPanelColumn(theme, theme.railFill());
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        sidebar.addChildren(
                sectionHeader(Component.translatable("screen.incore.shop.categories_heading"), theme),
                category == null
                        ? textLabel(Component.empty(), theme.secondaryText(), true)
                        : ShopAppUiSupport.currencyValue(
                                category.currency(),
                                ShopAppUiSupport.availableCurrencyAmount(category.currency()),
                                theme.priceText()
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
                dualSectionHeader(title, Component.translatable("screen.incore.shop.remaining_feed"), theme),
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
                dualSectionHeader(title, Component.literal(offer.displayName()), theme),
                ShopAppDetailsView.create(context, theme, offer, false)
        );
        return dock;
    }

    private static UIElement arcadePurchaseDock(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        ShopService.OfferView offer = arcadeSelectedOffer(context, context.data(), null);
        UIElement dock = fillPanelColumn(theme, theme.accentSoftFill());
        dock.addChildren(
                dualSectionHeader(
                        Component.translatable("screen.incore.shop.selection_rail"),
                        Component.translatable("screen.incore.shop.selection_rail_subtitle"),
                        theme
                )
        );
        if (offer == null) {
            dock.addChild(ShopAppDetailsView.createEmpty(
                    theme,
                    Component.translatable("screen.incore.shop.selected_offer"),
                    Component.translatable("screen.incore.shop.no_offer_selected")
            ));
            return dock;
        }
        dock.addChildren(
                heroPanel(context, theme, offer, Component.translatable("screen.incore.shop.selected_offer")),
                ShopAppDetailsView.create(context, theme, offer, false)
        );
        return dock;
    }

    private static @Nullable ShopService.OfferView arcadeSelectedOffer(
            ShopAppLayoutContext context,
            ShopService.ScreenData data,
            @Nullable ShopService.OfferView fallbackOffer
    ) {
        ShopService.OfferView selected = context.state().effectiveSelectedOffer(data);
        if (selected != null) {
            return selected;
        }
        return fallbackOffer;
    }

    private static UIElement selectionSummary(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        var offer = context.state().selectedOffer(context.data());
        UIElement column = panelColumn(theme, theme.accentSoftFill());
        column.addChildren(
                dualSectionHeader(
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
                currencyMetricTile(
                        Component.translatable("screen.incore.shop.price_label"),
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme,
                        false,
                        theme.priceText()
                ),
                metricTile(Component.translatable("screen.incore.shop.stock_label"), Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme)
        );
        return column;
    }

    private static UIElement arcadeCategoryRail(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement rail = fillPanelColumn(theme, theme.railFill());
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        rail.addChildren(
                dualSectionHeader(
                        Component.translatable("screen.incore.shop.categories_heading"),
                        category == null ? Component.empty() : Component.literal(category.displayName()),
                        theme
                ),
                category == null
                        ? textLabel(Component.empty(), theme.secondaryText(), true)
                        : ShopAppUiSupport.currencyValue(
                                category.currency(),
                                ShopAppUiSupport.availableCurrencyAmount(category.currency()),
                                theme.priceText()
                        ),
                categoryScroller(context, theme)
        );
        return rail;
    }

    private static UIElement abyssalCategoryRail(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme) {
        UIElement rail = fillPanelColumn(theme, theme.railFill());
        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), context.state().selectedCategoryId());
        rail.addChildren(
                dualSectionHeader(
                        Component.translatable("screen.incore.shop.categories_heading"),
                        category == null ? Component.empty() : Component.literal(category.displayName()),
                        theme
                ),
                category == null
                        ? textLabel(Component.empty(), theme.secondaryText(), true)
                        : ShopAppUiSupport.currencyValue(
                                category.currency(),
                                ShopAppUiSupport.availableCurrencyAmount(category.currency()),
                                theme.priceText()
                        ),
                categoryScroller(context, theme)
        );
        return rail;
    }

    private static UIElement archiveRailOfferDisplay(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        Button card = new Button().setText(Component.empty());
        card.text.setDisplay(false);
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(220);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.STRETCH);
            layout.gapAll(8);
            layout.paddingAll(10);
        });
        card.buttonStyle(style -> style
                .baseTexture(ShopAppUiSupport.framedTexture(theme.sectionFill(), theme.accentDivider()))
                .hoverTexture(ShopAppUiSupport.framedTexture(theme.cardHoverFill(), theme.accentDivider()))
                .pressedTexture(ShopAppUiSupport.framedTexture(theme.cardSelectedFill(), theme.accent()))
        );
        card.addChildren(
                ShopAppUiSupport.tintedSurface(theme, theme.insetFill(), 10, true).layout(layout -> {
                    layout.widthPercent(100);
                    layout.height(136);
                    layout.alignItems(AlignItems.CENTER);
                    layout.justifyContent(AlignContent.CENTER);
                }).addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 68, Component.literal(offer.displayName()))),
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true),
                textLabel(heroMetaSubtitle(offer), theme.secondaryText(), true),
                compactCurrencyMetricRow(
                        Component.translatable("screen.incore.shop.price_label"),
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme,
                        theme.priceText()
                ),
                compactOfferMetricRow(
                        Component.translatable("screen.incore.shop.stock_label"),
                        Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())),
                        theme.secondaryText(),
                        theme.primaryText()
                )
        );
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
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
                    dualSectionHeader(title, Component.translatable("screen.incore.shop.no_offer_selected"), theme),
                    ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.workspace.empty"), theme.secondaryText())
            );
            return hero;
        }
        hero.addChildren(
                dualSectionHeader(title, Component.literal(offer.displayName()), theme),
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
        UIElement media = ShopAppUiSupport.liftedInsetSurface(theme, 10);
        media.layout(layout -> {
            layout.width(88);
            layout.height(88);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        media.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), 52, Component.literal(offer.displayName())));
        row.addChildren(
                media,
                offerText(offer, theme).layout(layout -> layout.flex(1)),
                offerBadge(offer, theme, false)
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
                currencyMetricTile(
                        Component.translatable("screen.incore.shop.balance_label"),
                        offer.currency(),
                        ShopAppUiSupport.availableCurrencyAmount(offer.currency()),
                        theme,
                        true,
                        theme.priceText()
                )
        );
        return row;
    }

    private static List<ShopService.OfferView> industrialRailOffers(ShopAppLayoutContext context) {
        ShopService.TabFeedView feed = ShopAppUiSupport.activeFeed(context.data(), context.state());
        return ShopAppUiSupport.visibleOffers(feed, context.state().offerScrollRow(), INDUSTRIAL_RAIL_ROWS);
    }

    private static UIElement industrialEditorialShowcase(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer
    ) {
        UIElement showcase = ShopAppUiSupport.tintedSurface(theme, theme.cardFill(), 8, true);
        showcase.layout(layout -> {
            layout.widthPercent(100);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });
        showcase.addChildren(
                textLabel(Component.translatable("screen.incore.shop.reward_contents"), theme.primaryText(), true),
                industrialRewardShowcase(offer, theme)
        );
        return showcase;
    }

    private static UIElement industrialStatsBox(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement box = ShopAppUiSupport.liftedInsetSurface(theme, 8);
        box.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });
        box.addChildren(
                ShopAppUiSupport.currencyMetricRow(
                        Component.translatable("screen.incore.shop.price_label"),
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme,
                        theme.priceText()
                ),
                inlineMetricRow(
                        Component.translatable("screen.incore.shop.stock_label"),
                        Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())),
                        theme
                )
        );
        if (offer.rotationRemainingMillis() >= 0L) {
            box.addChild(inlineMetricRow(
                    Component.translatable("screen.incore.shop.time_left_label"),
                    Component.literal(ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())),
                    theme
            ));
        }
        if (offer.rewardEntries().size() > 1) {
            box.addChild(inlineMetricRow(
                    Component.translatable("screen.incore.shop.bundle_label"),
                    Component.literal(ShopAppUiSupport.rewardSummary(offer)),
                    theme
            ));
        }
        return box;
    }

    private static UIElement inlineMetricRow(Component title, Component value, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        row.addChildren(
                textLabel(title, theme.secondaryText(), true).layout(layout -> {
                    layout.flexBasis(0);
                    layout.flexGrow(1);
                    layout.minWidth(0);
                }),
                valueLabel(value, theme.primaryText())
        );
        return row;
    }

    private static UIElement industrialRewardShowcase(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = ShopAppUiSupport.liftedInsetSurface(theme, 8);
        column.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(4);
        });
        int visibleEntries = Math.min(2, offer.rewardEntries().size());
        for (int i = 0; i < visibleEntries; i++) {
            ShopService.RewardEntryView entry = offer.rewardEntries().get(i);
            UIElement row = new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.flexDirection(FlexDirection.ROW);
                layout.alignItems(AlignItems.CENTER);
                layout.gapAll(6);
            });
            row.addChildren(
                    ShopAppUiSupport.itemIcon(
                            ShopAppUiSupport.stackFromRewardEntry(entry),
                            18,
                            Component.literal(ShopAppUiSupport.rewardEntryLabel(entry))
                    ),
                    textLabel(Component.literal(ShopAppUiSupport.rewardEntryLabel(entry)), theme.primaryText(), true).layout(layout -> {
                        layout.flex(1);
                        layout.minWidth(0);
                    })
            );
            column.addChild(row);
        }
        if (offer.rewardEntries().size() > visibleEntries) {
            column.addChild(textLabel(
                    Component.literal("+" + (offer.rewardEntries().size() - visibleEntries) + " more"),
                    theme.secondaryText(),
                    true
            ));
        }
        return column;
    }

    private static @Nullable ShopService.OfferView industrialSlotOffer(ShopAppLayoutContext context) {
        ShopService.OfferView showcase = context.state().showcaseOffer(context.data());
        if (showcase != null) {
            return showcase;
        }
        List<ShopService.OfferView> offers = industrialDisplayOffers(context);
        if (offers.isEmpty()) {
            return null;
        }
        return offers.getFirst();
    }

    private static List<ShopService.OfferView> industrialDisplayOffers(ShopAppLayoutContext context) {
        return ShopAppUiSupport.displayOffers(ShopAppUiSupport.activeFeed(context.data(), context.state()));
    }

    private static List<ShopService.OfferView> industrialHighlightOffers(ShopAppLayoutContext context) {
        ShopService.OfferView focus = industrialSlotOffer(context);
        List<ShopService.OfferView> offers = new ArrayList<>();
        for (ShopService.OfferView offer : industrialDisplayOffers(context)) {
            if (focus != null && offer.offerId().equals(focus.offerId())) {
                continue;
            }
            offers.add(offer);
            if (offers.size() >= 3) {
                break;
            }
        }
        return List.copyOf(offers);
    }

    private static List<ShopService.OfferView> industrialRemainingOffers(ShopAppLayoutContext context) {
        ShopService.OfferView focus = industrialSlotOffer(context);
        List<ShopService.OfferView> offers = new ArrayList<>();
        int skippedHighlights = 0;
        for (ShopService.OfferView offer : industrialDisplayOffers(context)) {
            if (focus != null && offer.offerId().equals(focus.offerId())) {
                continue;
            }
            if (skippedHighlights < 3) {
                skippedHighlights++;
                continue;
            }
            offers.add(offer);
        }
        return List.copyOf(offers);
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
        UIElement column = new UIElement();
        column.layout(layout -> {
            layout.widthPercent(100);
            layout.minHeight(0);
            layout.gapAll(6);
        });
        column.addChildren(
                sectionHeader(title, theme),
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
            (i % 2 == 0 ? left : right).addChild(offerCard(context, offers.get(i), theme, i % 3 == 0 ? 78 : 68, true));
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
            (i % 2 == 0 ? left : right).addChild(offerCard(context, offers.get(i), theme, compact ? 40 : 48, compact));
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
            column.addChild(offerCard(context, offer, theme, compact ? 40 : 48, compact));
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
        boolean selected = showOfferSelectionState(context) && offer.offerId().equals(context.state().selectedOfferId());
        Button card = new Button().setText(Component.empty());
        card.layout(layout -> {
            layout.widthPercent(100);
            layout.height(height);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.FLEX_START);
            layout.gapAll(compact ? 4 : 6);
            layout.paddingAll(compact ? 4 : 6);
        });
        card.text.setDisplay(false);
        ShopAppUiSupport.styleSelectable(card, theme, selected);
        card.addChildren(
                ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), compact ? 16 : 18, Component.literal(offer.displayName())),
                offerText(offer, theme).layout(layout -> layout.flex(1)),
                offerCardBadge(offer, theme, compact)
        );
        card.setOnClick(event -> {
            context.state().openDetails(offer.offerId(), context.data());
            context.rebuild();
        });
        return card;
    }

    private static boolean showOfferSelectionState(ShopAppLayoutContext context) {
        return ShopAppUiSupport.detailsModeFor(context.data(), context.state().activeTab()) != ShopDetailsPresentationMode.MODAL_OVERLAY;
    }

    private static UIElement offerText(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        UIElement column = new UIElement().layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(1);
        });
        column.addChildren(
                textLabel(Component.literal(offer.displayName()), theme.primaryText(), true)
        );
        if (offer.rotationRemainingMillis() >= 0L) {
            column.addChild(textLabel(
                    Component.translatable("screen.incore.shop.time_left_format", ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())),
                    theme.secondaryText(),
                    true
            ));
        }
        if (offer.rewardEntries().size() > 1) {
            column.addChild(textLabel(
                    Component.literal(ShopAppUiSupport.rewardSummary(offer)),
                    theme.secondaryText(),
                    true
            ));
        }
        return column;
    }

    private static @Nullable Label optionalHeaderMetaLabel(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme) {
        String text = offer.rotationRemainingMillis() >= 0L
                ? ShopAppUiSupport.rotationRemainingLabel(offer.rotationRemainingMillis())
                : offer.rewardEntries().size() > 1 ? ShopAppUiSupport.rewardSummary(offer) : "";
        if (text.isBlank()) {
            return null;
        }
        Label label = textLabel(Component.literal(text), theme.secondaryText(), true);
        label.layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
        });
        label.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
        return label;
    }

    private static UIElement offerBadge(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme, boolean compact) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(compact ? 126 : 144);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(2);
            layout.paddingRight(4);
        });
        if (compact) {
            column.addChild(ShopAppUiSupport.currencyValue(
                    offer.currency(),
                    ShopAppUiSupport.currencyAmount(offer.currency()),
                    theme.priceText()
            ));
            column.addChild(textLabel(
                    Component.translatable("screen.incore.shop.stock.remaining", ShopAppUiSupport.stockLabel(offer.availableStock())),
                    theme.secondaryText(),
                    true
            ));
            return column;
        }
        column.addChild(compactCurrencyMetricRow(
                Component.translatable("screen.incore.shop.price_label"),
                offer.currency(),
                ShopAppUiSupport.currencyAmount(offer.currency()),
                theme,
                theme.priceText()
        ));
        column.addChild(compactOfferMetricRow(
                Component.translatable("screen.incore.shop.stock_label"),
                Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())),
                theme.secondaryText(),
                theme.primaryText()
        ));
        return column;
    }

    private static UIElement offerCardBadge(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme, boolean compact) {
        UIElement column = new UIElement().layout(layout -> {
            layout.width(compact ? 112 : 138);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.alignItems(AlignItems.FLEX_END);
            layout.gapAll(compact ? 1 : 2);
            layout.paddingRight(compact ? 4 : 4);
        });
        column.addChild(ShopAppUiSupport.currencyValue(
                offer.currency(),
                ShopAppUiSupport.currencyAmount(offer.currency()),
                theme.priceText()
        ));
        column.addChild(valueLabel(
                Component.translatable("screen.incore.shop.stock.remaining", ShopAppUiSupport.stockLabel(offer.availableStock())),
                theme.secondaryText()
        ));
        return column;
    }

    private static UIElement compactOfferMetricRow(Component title, Component value, int titleColor, int valueColor) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(4);
        });
        row.addChildren(
                textLabel(title, titleColor, true).layout(layout -> layout.width(34)),
                valueLabel(value, valueColor).layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                })
        );
        return row;
    }

    private static UIElement compactCurrencyMetricRow(
            Component title,
            ShopService.CurrencyView currency,
            int amount,
            ShopAppUiSupport.TabTheme theme,
            int valueColor
    ) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(4);
        });
        row.addChildren(
                textLabel(title, theme.secondaryText(), true).layout(layout -> layout.width(34)),
                ShopAppUiSupport.currencyValue(currency, amount, valueColor).layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                })
        );
        return row;
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

    private static UIElement currencyMetricTile(
            Component title,
            ShopService.CurrencyView currency,
            int amount,
            ShopAppUiSupport.TabTheme theme,
            boolean lifted,
            int valueColor
    ) {
        return ShopAppUiSupport.currencyMetricTile(title, currency, amount, theme, lifted, valueColor);
    }

    private static UIElement showcaseMetricTile(Component title, Component value, ShopAppUiSupport.TabTheme theme) {
        UIElement tile = ShopAppUiSupport.liftedInsetSurface(theme, 5);
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

    private static UIElement sectionHeader(Component title, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(1);
        });
        row.addChildren(
                textLabel(title, theme.primaryText(), true)
        );
        return row;
    }

    private static UIElement dualSectionHeader(Component title, Component subtitle, ShopAppUiSupport.TabTheme theme) {
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

    private static Label valueLabel(Component text, int color) {
        Label label = ShopAppUiSupport.heading(text, color);
        label.textStyle(style -> style
                .adaptiveWidth(true)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.RIGHT)
                .textColor(color)
        );
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

    private static UIElement selectedCategoryLockedDisplay(
            ShopAppUiSupport.TabTheme theme,
            ShopService.CategoryView category
    ) {
        UIElement panel = panelColumn(theme, theme.sectionFill());
        panel.layout(layout -> {
            layout.heightPercent(100);
            layout.minWidth(0);
        });
        panel.addChild(ShopAppUiSupport.lockedCategoryDisplay(theme, category).layout(layout -> {
            layout.flex(1);
            layout.minHeight(0);
        }));
        return panel;
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
