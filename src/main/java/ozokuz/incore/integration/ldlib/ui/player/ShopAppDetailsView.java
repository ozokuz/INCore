package ozokuz.incore.integration.ldlib.ui.player;

import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import net.minecraft.network.chat.Component;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.network.ShopNetworking;

final class ShopAppDetailsView {
    private ShopAppDetailsView() {
    }

    static UIElement create(
            ShopAppLayoutContext context,
            ShopAppUiSupport.TabTheme theme,
            ShopService.OfferView offer,
            boolean modal
    ) {
        boolean compactInline = !modal;
        UIElement column = modal
                ? ShopAppUiSupport.tintedSurface(theme, theme.sectionFill(), 10, true)
                : ShopAppUiSupport.accentSurface(theme, 6);
        column.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(compactInline ? 4 : 6);
        });

        ShopService.CategoryView category = ShopAppUiSupport.findCategory(context.data(), offer.categoryId());
        int totalCost = offer.price() * context.state().quantity();
        boolean canPurchase = !offer.locked()
                && (offer.availableStock() < 0 || offer.availableStock() > 0)
                && offer.currency().availableAmount() >= totalCost;

        UIElement header = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(compactInline ? 4 : 6);
        });
        header.addChildren(
                previewFrame(offer, theme, modal ? 54 : 36),
                new UIElement().layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                    layout.flexDirection(FlexDirection.COLUMN);
                    layout.gapAll(2);
                }).addChildren(
                        titleLabel(Component.literal(offer.displayName()), theme.primaryText()),
                        ShopAppUiSupport.bodyLabel(Component.literal(ShopAppUiSupport.rewardSummary(offer)), theme.secondaryText())
                )
        );
        if (modal) {
            Button close = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.close_overlay"), theme, 20);
            close.layout(layout -> layout.width(74));
            close.setOnClick(event -> {
                context.state().closeDetails();
                context.rebuild();
            });
            header.addChild(close);
        }

        column.addChildren(
                header,
                metricRow(Component.translatable("screen.incore.shop.category_label"), Component.literal(category == null ? "" : category.displayName()), theme),
                ShopAppUiSupport.currencyMetricRow(
                        Component.translatable("screen.incore.shop.price_label"),
                        offer.currency(),
                        ShopAppUiSupport.currencyAmount(offer.currency()),
                        theme,
                        theme.priceText()
                ),
                ShopAppUiSupport.currencyMetricRow(
                        Component.translatable("screen.incore.shop.balance_label"),
                        offer.currency(),
                        ShopAppUiSupport.availableCurrencyAmount(offer.currency()),
                        theme,
                        theme.priceText()
                ),
                metricRow(Component.translatable("screen.incore.shop.stock_label"), Component.literal(ShopAppUiSupport.stockLabel(offer.availableStock())), theme),
                rewardContents(offer, theme, compactInline)
        );

        column.addChildren(
                quantityControls(context, theme, compactInline),
                ShopAppUiSupport.currencyMetricRow(
                        Component.translatable("screen.incore.shop.total_cost_label"),
                        offer.currency(),
                        totalCost,
                        theme,
                        theme.priceText()
                )
        );

        if (offer.locked()) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.locked"), theme.alertText()));
        } else if (offer.availableStock() == 0) {
            column.addChild(ShopAppUiSupport.bodyLabel(Component.translatable("screen.incore.shop.out_of_stock_inline"), theme.alertText()));
        }

        Button purchase = ShopAppUiSupport.actionButton(Component.translatable("screen.incore.shop.purchase"), theme, compactInline ? 18 : 20);
        purchase.layout(layout -> layout.widthPercent(100));
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
        column.addChild(purchase);
        return column;
    }

    static UIElement createEmpty(ShopAppUiSupport.TabTheme theme, Component title, Component subtitle) {
        UIElement empty = ShopAppUiSupport.accentSurface(theme, 8);
        empty.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(6);
            layout.justifyContent(AlignContent.CENTER);
        });
        empty.addChildren(
                titleLabel(title, theme.primaryText()),
                ShopAppUiSupport.bodyLabel(subtitle, theme.secondaryText())
        );
        return empty;
    }

    private static UIElement previewFrame(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme, int size) {
        UIElement frame = ShopAppUiSupport.liftedInsetSurface(theme, 4);
        frame.layout(layout -> {
            layout.width(size + 12);
            layout.height(size + 12);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        frame.addChild(ShopAppUiSupport.itemIcon(ShopAppUiSupport.stackForOffer(offer), size, Component.literal(offer.displayName())));
        return frame;
    }

    private static UIElement quantityControls(ShopAppLayoutContext context, ShopAppUiSupport.TabTheme theme, boolean compactInline) {
        UIElement row = ShopAppUiSupport.highlightedSurface(theme, 6);
        row.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.gapAll(compactInline ? 4 : 6);
        });

        Button decrease = ShopAppUiSupport.actionButton(Component.literal("-"), theme, compactInline ? 16 : 18);
        decrease.layout(layout -> layout.width(compactInline ? 20 : 24));
        decrease.setActive(context.state().quantity() > 1);
        decrease.setOnClick(event -> {
            context.state().decreaseQuantity();
            context.rebuild();
        });

        Label quantity = ShopAppUiSupport.heading(Component.translatable("screen.incore.shop.quantity", context.state().quantity()), theme.primaryText());
        quantity.layout(layout -> layout.flex(1));
        quantity.textStyle(style -> style.textAlignHorizontal(Horizontal.CENTER));

        Button increase = ShopAppUiSupport.actionButton(Component.literal("+"), theme, compactInline ? 16 : 18);
        increase.layout(layout -> layout.width(compactInline ? 20 : 24));
        increase.setActive(context.state().quantity() < context.state().quantityMax(context.data()));
        increase.setOnClick(event -> {
            context.state().increaseQuantity(context.data());
            context.rebuild();
        });

        row.addChildren(decrease, quantity, increase);
        return row;
    }

    private static UIElement rewardContents(ShopService.OfferView offer, ShopAppUiSupport.TabTheme theme, boolean compactInline) {
        UIElement column = ShopAppUiSupport.liftedInsetSurface(theme, 6);
        column.layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.COLUMN);
            layout.gapAll(compactInline ? 3 : 4);
        });
        column.addChildren(
                titleLabel(Component.translatable("screen.incore.shop.reward_contents"), theme.primaryText()),
                ShopAppUiSupport.bodyLabel(Component.literal(ShopAppUiSupport.rewardSummary(offer)), theme.secondaryText())
        );
        for (ShopService.RewardEntryView entry : offer.rewardEntries()) {
            UIElement row = new UIElement().layout(layout -> {
                layout.widthPercent(100);
                layout.flexDirection(FlexDirection.ROW);
                layout.alignItems(AlignItems.CENTER);
                layout.gapAll(6);
            });
            row.addChildren(
                    ShopAppUiSupport.itemIcon(
                            ShopAppUiSupport.stackFromRewardEntry(entry),
                            16,
                            Component.literal(ShopAppUiSupport.rewardEntryLabel(entry))
                    ),
                    titleLabel(Component.literal(ShopAppUiSupport.rewardEntryLabel(entry)), theme.primaryText()).layout(layout -> layout.flex(1)),
                    titleLabel(Component.literal("x" + entry.count()), theme.secondaryText())
            );
            column.addChild(row);
        }
        return column;
    }

    private static UIElement metricRow(Component left, Component right, ShopAppUiSupport.TabTheme theme) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        Label leftLabel = titleLabel(left, theme.secondaryText());
        leftLabel.layout(layout -> {
            layout.width(84);
        });
        leftLabel.textStyle(style -> style.adaptiveWidth(false).textWrap(TextWrap.HIDE).textAlignHorizontal(Horizontal.LEFT));
        Label rightLabel = titleLabel(right, theme.primaryText());
        rightLabel.layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
        });
        rightLabel.textStyle(style -> style.textAlignHorizontal(Horizontal.RIGHT));
        row.addChildren(leftLabel, rightLabel);
        return row;
    }

    private static Label titleLabel(Component text, int color) {
        return ShopAppUiSupport.heading(text, color);
    }
}
