package ozokuz.incore.integration.ldlib.ui.player;

import com.google.gson.Gson;
import com.lowdragmc.lowdraglib2.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Horizontal;
import com.lowdragmc.lowdraglib2.gui.ui.data.TextWrap;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.AlignContent;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.FlexDirection;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.shop.ShopDetailsPresentationMode;
import ozokuz.incore.features.shop.ShopLayoutId;
import ozokuz.incore.features.shop.ShopPaletteId;
import ozokuz.incore.features.shop.ShopService;
import ozokuz.incore.features.shop.ShopTabId;
import ozokuz.incore.integration.ldlib.ui.texture.BeveledRectTexture;

final class ShopAppUiSupport {
    private static final Gson GSON = new Gson();

    static final int TARGET_WINDOW_WIDTH = 920;
    static final int TARGET_WINDOW_HEIGHT = 520;
    static final int MIN_WINDOW_WIDTH = 680;
    static final int MIN_WINDOW_HEIGHT = 400;
    static final int VISIBLE_OFFER_ROWS = 6;

    private ShopAppUiSupport() {
    }

    static ShopService.ScreenData emptyData() {
        return new ShopService.ScreenData("", "", List.of(), List.of(), List.of());
    }

    static ShopService.ScreenData parse(String json) {
        ShopService.ScreenData parsed = GSON.fromJson(json, ShopService.ScreenData.class);
        if (parsed == null || parsed.tabs() == null || parsed.categories() == null || parsed.offers() == null) {
            return emptyData();
        }
        return new ShopService.ScreenData(
                parsed.selectedCategoryId() == null ? "" : parsed.selectedCategoryId(),
                parsed.selectedOfferId() == null ? "" : parsed.selectedOfferId(),
                parsed.tabs(),
                parsed.categories(),
                parsed.offers()
        );
    }

    static List<ShopService.TabView> orderedTabs(ShopService.ScreenData data) {
        return data.tabs();
    }

    static List<ShopService.CategoryView> orderedCategories(ShopService.ScreenData data) {
        List<ShopService.CategoryView> ordered = new ArrayList<>();
        for (ShopService.TabView tab : data.tabs()) {
            ordered.addAll(categoriesForTab(data, ShopTabId.fromString(tab.tabId())));
        }
        return List.copyOf(ordered);
    }

    static @Nullable ShopService.TabView findTab(ShopService.ScreenData data, ShopTabId tabId) {
        for (ShopService.TabView tab : data.tabs()) {
            if (tab.tabId().equals(tabId.serialized())) {
                return tab;
            }
        }
        return null;
    }

    static List<ShopService.CategoryView> categoriesForTab(ShopService.ScreenData data, ShopTabId tabId) {
        return ShopService.orderedCategoriesForTab(data, tabId);
    }

    static ShopService.TabFeedView feedForTab(ShopService.ScreenData data, ShopTabId tabId, @Nullable String categoryId) {
        return ShopService.buildTabFeed(data, tabId, categoryId);
    }

    static ShopService.TabFeedView activeFeed(ShopService.ScreenData data, ShopAppUiState state) {
        return feedForTab(data, state.activeTab(), state.selectedCategoryId());
    }

    static @Nullable ShopService.CategoryView findCategory(ShopService.ScreenData data, @Nullable String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        for (ShopService.CategoryView category : data.categories()) {
            if (categoryId.equals(category.categoryId())) {
                return category;
            }
        }
        return null;
    }

    static @Nullable ShopService.OfferView findOffer(ShopService.ScreenData data, @Nullable String offerId) {
        if (offerId == null || offerId.isBlank()) {
            return null;
        }
        for (ShopService.OfferView offer : data.offers()) {
            if (offerId.equals(offer.offerId())) {
                return offer;
            }
        }
        return null;
    }

    static ShopTabId tabForCategory(ShopService.ScreenData data, ShopService.CategoryView category) {
        return tabForCategoryId(data, category.categoryId());
    }

    static ShopTabId tabForCategoryId(ShopService.ScreenData data, @Nullable String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return ShopTabId.INDUSTRIAL_MARKET;
        }
        for (ShopService.TabView tab : data.tabs()) {
            if (tab.categoryIds().contains(categoryId)) {
                return ShopTabId.fromString(tab.tabId());
            }
        }
        return ShopTabId.INDUSTRIAL_MARKET;
    }

    static ShopDetailsPresentationMode detailsModeFor(ShopService.ScreenData data, ShopTabId tabId) {
        ShopService.TabView tab = findTab(data, tabId);
        return tab == null
                ? ShopDetailsPresentationMode.INLINE_DOCK
                : ShopDetailsPresentationMode.fromString(tab.detailsMode());
    }

    static TabTheme themeFor(ShopService.ScreenData data, ShopTabId tabId) {
        ShopService.TabView tab = findTab(data, tabId);
        ShopPaletteId paletteId = tab == null ? ShopPaletteId.TACTICAL_ARCHIVE : ShopPaletteId.fromString(tab.paletteId());
        return switch (paletteId) {
            case TACTICAL_ARCHIVE -> new TabTheme(0x7A0E0F09, 0xFF14140E, 0xFF35352E, 0xFFA2AD7E, 0xFFC1CC9A, 0xFFD4C58D, 0xFF9EA36F, 0xFFC97A5B, 0xFF0E0F09, 0xFF202016);
            case STEEL_AEGIS -> new TabTheme(0x7A000000, 0xFF1B2027, 0xFF384955, 0xFFB7C9D8, 0xFFF9F9F9, 0xFFB7C9D8, 0xFF8FA2B2, 0xFFEE7D77, 0xFF0C0E11, 0xFF232E37);
            case OBSIDIAN_EMBER -> new TabTheme(0x7A0E0E0E, 0xFF20201F, 0xFF544339, 0xFFFB8F00, 0xFFE5E2E1, 0xFFFFB778, 0xFFDDC1AE, 0xFFFFB4AB, 0xFF131313, 0xFF353535);
            case NEON_SHADOW -> new TabTheme(0x7A000000, 0xFF121220, 0xFF474656, 0xFFD978FF, 0xFFE9E6F9, 0xFF81ECFF, 0xFFB9B3CF, 0xFFFF51FA, 0xFF0D0D1A, 0xFF242437);
            case BLOOD_PROTOCOL -> new TabTheme(0x7A000000, 0xFF1C1B1B, 0xFF5B403C, 0xFF980100, 0xFFFFB4A8, 0xFFE4BEB8, 0xFFC89E98, 0xFFFFB4AB, 0xFF131313, 0xFF353534);
            case ABYSSAL_PROTOCOL -> new TabTheme(0x7A0C0F10, 0xFF111415, 0xFF41484A, 0xFF003333, 0xFF00FBFB, 0xFF00DDDD, 0xFF7ED5D5, 0xFFFFB4AB, 0xFF0C0F10, 0xFF272A2B);
        };
    }

    static ShopAppLayout layoutFor(ShopService.ScreenData data, ShopTabId tabId) {
        ShopService.TabView tab = findTab(data, tabId);
        ShopLayoutId layoutId = tab == null ? ShopLayoutId.INDUSTRIAL_MARKET : ShopLayoutId.fromString(tab.layoutId());
        return ShopAppLayouts.forLayout(layoutId);
    }

    static List<ShopService.OfferView> visibleOffers(ShopService.TabFeedView feed, int start, int rows) {
        if (feed.remainingOffers().isEmpty()) {
            return List.of();
        }
        int safeStart = Math.min(start, Math.max(0, feed.remainingOffers().size() - 1));
        int end = Math.min(feed.remainingOffers().size(), safeStart + rows);
        return feed.remainingOffers().subList(safeStart, end);
    }

    static List<ShopService.OfferView> displayOffers(ShopService.TabFeedView feed) {
        List<ShopService.OfferView> display = new ArrayList<>(feed.showcaseOffers());
        display.addAll(feed.remainingOffers());
        return List.copyOf(display);
    }

    static ItemStack stackForOffer(ShopService.OfferView offer) {
        if (offer.rewardEntries().isEmpty()) {
            return ItemStack.EMPTY;
        }
        ShopService.RewardEntryView entry = offer.rewardEntries().getFirst();
        return stackFromSpec(entry.stackSpec(), Math.max(1, entry.count()));
    }

    static ItemStack stackFromRewardEntry(ShopService.RewardEntryView entry) {
        return stackFromSpec(entry.stackSpec(), entry.count());
    }

    static ItemStack stackFromSpec(@Nullable String stackSpec, int count) {
        if (stackSpec == null || stackSpec.isBlank()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = ShopService.parseStack(stackSpec, count);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    static ShopService.CurrencyView emptyCurrencyView() {
        return new ShopService.CurrencyView("minecraft:barrier", "", 1, 0);
    }

    static String stockLabel(int stock) {
        return stock < 0
                ? "\u221e"
                : Integer.toString(stock);
    }

    static int currencyAmount(ShopService.CurrencyView currency) {
        return Math.max(1, currency.amountPerUnit());
    }

    static int availableCurrencyAmount(ShopService.CurrencyView currency) {
        return Math.max(0, currency.availableAmount());
    }

    static ItemStack stackFromCurrency(ShopService.CurrencyView currency) {
        ResourceLocation itemId = ResourceLocation.tryParse(currency.iconItemId());
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return new ItemStack(Items.BARRIER);
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        return item == Items.AIR ? new ItemStack(Items.BARRIER) : item.getDefaultInstance();
    }

    static UIElement currencyValue(ShopService.CurrencyView currency, int amount, int color) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthAuto();
            layout.flexDirection(FlexDirection.ROW);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.FLEX_END);
            layout.gapAll(3);
        });
        Component tooltip = currency.label() == null || currency.label().isBlank() ? null : Component.literal(currency.label());
        UIElement icon = tooltip == null
                ? itemIcon(stackFromCurrency(currency), 16)
                : itemIcon(stackFromCurrency(currency), 16, tooltip);
        Label value = heading(Component.literal(Integer.toString(Math.max(0, amount))), color);
        value.textStyle(style -> style
                .adaptiveWidth(true)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
                .textColor(color)
        );
        row.addChildren(icon, value);
        return row;
    }

    static UIElement currencyMetricRow(Component title, ShopService.CurrencyView currency, int amount, TabTheme theme, int valueColor) {
        UIElement row = new UIElement().layout(layout -> {
            layout.widthPercent(100);
            layout.flexDirection(FlexDirection.ROW);
            layout.justifyContent(AlignContent.SPACE_BETWEEN);
            layout.alignItems(AlignItems.CENTER);
            layout.gapAll(6);
        });
        Label leftLabel = heading(title, theme.secondaryText());
        leftLabel.layout(layout -> layout.width(84));
        leftLabel.textStyle(style -> style.adaptiveWidth(false).textWrap(TextWrap.HIDE).textAlignHorizontal(Horizontal.LEFT));
        row.addChildren(
                leftLabel,
                new UIElement().layout(layout -> {
                    layout.flex(1);
                    layout.minWidth(0);
                    layout.flexDirection(FlexDirection.ROW);
                    layout.justifyContent(AlignContent.FLEX_END);
                }).addChild(currencyValue(currency, amount, valueColor))
        );
        return row;
    }

    static UIElement currencyMetricTile(
            Component title,
            ShopService.CurrencyView currency,
            int amount,
            TabTheme theme,
            boolean lifted,
            int valueColor
    ) {
        UIElement tile = lifted ? liftedInsetSurface(theme, 5) : mutedSurface(theme, 5);
        tile.layout(layout -> {
            layout.flex(1);
            layout.minWidth(0);
            layout.alignItems(AlignItems.FLEX_START);
        });
        tile.addChildren(
                heading(title, theme.secondaryText()),
                currencyValue(currency, amount, valueColor)
        );
        return tile;
    }

    static String rewardSummary(ShopService.OfferView offer) {
        int totalCount = offer.rewardEntries().stream().mapToInt(ShopService.RewardEntryView::count).sum();
        if (offer.rewardEntries().size() <= 1) {
            return Component.translatable("screen.incore.shop.reward_count", totalCount).getString();
        }
        return Component.translatable(
                "screen.incore.shop.bundle_summary",
                offer.rewardEntries().size(),
                totalCount
        ).getString();
    }

    static String rotationRemainingLabel(long remainingMillis) {
        if (remainingMillis < 0L) {
            return "0m";
        }
        long totalSeconds = Math.max(0L, remainingMillis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        if (hours > 0L) {
            return Component.translatable("screen.incore.shop.rotation_remaining.hours_minutes", hours, minutes).getString();
        }
        return Component.translatable("screen.incore.shop.rotation_remaining.minutes", minutes).getString();
    }

    static String rewardEntryLabel(ShopService.RewardEntryView entry) {
        ItemStack stack = stackFromRewardEntry(entry);
        return stack.isEmpty() ? entry.stackSpec() : stack.getHoverName().getString();
    }

    static Button actionButton(Component text, TabTheme theme, int height) {
        Button button = new Button().setText(text);
        button.layout(layout -> {
            layout.height(height);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        button.text.getLayout().flex(1);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
                .textColor(theme.primaryText())
        );
        button.buttonStyle(style -> style
                .baseTexture(framedTexture(theme.buttonFill(), theme.accentDivider()))
                .hoverTexture(framedTexture(theme.buttonHoverFill(), theme.accent()))
                .pressedTexture(framedTexture(theme.buttonPressedFill(), theme.accent()))
        );
        return button;
    }

    static Button chipButton(Component text, TabTheme theme, boolean active) {
        Button button = actionButton(text, theme, 20);
        button.layout(layout -> {
            layout.minWidth(88);
            layout.paddingHorizontal(6);
        });
        styleSelectable(button, theme, active);
        return button;
    }

    static Button tabButton(Component text, TabTheme theme, boolean active) {
        Button button = new Button().setText(text);
        button.layout(layout -> {
            layout.height(active ? 24 : 20);
            layout.minWidth(104);
            layout.paddingHorizontal(10);
            layout.alignItems(AlignItems.CENTER);
            layout.justifyContent(AlignContent.CENTER);
        });
        button.text.getLayout().flex(1);
        button.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.CENTER)
                .textColor(active ? 0xFFFFFFFF : theme.secondaryText())
        );
        if (active) {
            button.buttonStyle(style -> style
                    .baseTexture(framedTexture(theme.tabActiveFill(), theme.accentDivider()))
                    .hoverTexture(framedTexture(theme.tabActiveFill(), theme.accentDivider()))
                    .pressedTexture(framedTexture(theme.buttonPressedFill(), theme.accent()))
            );
        } else {
            button.buttonStyle(style -> style
                    .baseTexture(softTexture(theme.tabFill()))
                    .hoverTexture(softTexture(theme.tabHoverFill()))
                    .pressedTexture(softTexture(theme.sectionFill()))
            );
        }
        return button;
    }

    static void styleSelectable(Button button, TabTheme theme, boolean active) {
        if (active) {
            button.buttonStyle(style -> style
                    .baseTexture(framedTexture(theme.cardSelectedFill(), theme.accentDivider()))
                    .hoverTexture(framedTexture(theme.cardSelectedFill(), theme.accentDivider()))
                    .pressedTexture(framedTexture(theme.buttonPressedFill(), theme.accent()))
            );
            button.text.textStyle(style -> style.textColor(0xFFFFFFFF));
        } else {
            button.buttonStyle(style -> style
                    .baseTexture(softTexture(theme.cardFill()))
                    .hoverTexture(softTexture(theme.cardHoverFill()))
                    .pressedTexture(softTexture(theme.sectionFill()))
            );
        }
    }

    static UIElement surface(TabTheme theme, int padding, int radius) {
        return tintedSurface(theme, theme.sectionFill(), padding, radius > 0);
    }

    static UIElement mutedSurface(TabTheme theme, int padding) {
        return tintedSurface(theme, mixColor(theme.insetFill(), theme.sectionFill(), 0.24F), padding, false);
    }

    static UIElement liftedInsetSurface(TabTheme theme, int padding) {
        UIElement element = new UIElement();
        element.layout(layout -> {
            layout.gapAll(6);
            layout.paddingAll(padding);
        });
        int fill = mixColor(theme.sectionFill(), theme.primaryText(), 0.10F);
        element.style(style -> style.backgroundTexture(softTexture(fill)));
        return element;
    }

    static UIElement accentSurface(TabTheme theme, int padding) {
        return tintedSurface(theme, theme.accentSoftFill(), padding, false);
    }

    static UIElement highlightedSurface(TabTheme theme, int padding) {
        return tintedSurface(theme, theme.accentFill(), padding, false);
    }

    static UIElement tintedSurface(TabTheme theme, int fill, int padding, boolean outlined) {
        UIElement element = new UIElement();
        element.layout(layout -> {
            layout.gapAll(6);
            layout.paddingAll(padding);
        });
        element.style(style -> style.backgroundTexture(outlined ? framedTexture(fill, theme.divider()) : softTexture(fill)));
        return element;
    }

    static Label heading(Component text, int color) {
        Label label = new Label();
        label.setText(text);
        label.textStyle(style -> style
                .adaptiveWidth(false)
                .textWrap(TextWrap.HIDE)
                .textAlignHorizontal(Horizontal.LEFT)
                .textColor(color)
        );
        return label;
    }

    static Label bodyLabel(Component text, int color) {
        Label label = new Label();
        label.setText(text);
        label.textStyle(style -> style
                .adaptiveHeight(true)
                .textWrap(TextWrap.WRAP)
                .textColor(color)
        );
        return label;
    }

    static UIElement itemIcon(ItemStack stack, int size, @Nullable Component... tooltips) {
        UIElement icon = new UIElement().layout(layout -> {
            layout.width(size);
            layout.height(size);
        });
        icon.style(style -> style.backgroundTexture(new ItemStackTexture(stack)));
        if (tooltips != null && tooltips.length > 0) {
            icon.style(style -> style.tooltips(tooltips));
        }
        return icon;
    }

    static IGuiTexture buttonTexture(int fill, int borderTop, int borderBottom, int radius) {
        return new BeveledRectTexture(fill, borderTop, borderBottom, borderBottom, radius, 1);
    }

    static IGuiTexture softTexture(int fill) {
        return new BeveledRectTexture(
                fill,
                fill,
                brightenColor(fill, 0.04F),
                darkenColor(fill, 0.14F),
                0,
                1
        );
    }

    static IGuiTexture framedTexture(int fill, int outline) {
        return new BeveledRectTexture(
                fill,
                outline,
                brightenColor(fill, 0.05F),
                darkenColor(fill, 0.18F),
                1,
                1
        );
    }

    static IGuiTexture flatTexture(int fill) {
        return new BeveledRectTexture(fill, fill, fill, fill, 0, 0);
    }

    static int brightenColor(int argb, float amount) {
        return mixColor(argb, 0xFFFFFFFF, amount);
    }

    static int darkenColor(int argb, float amount) {
        return mixColor(argb, 0xFF000000, amount);
    }

    static int mixColor(int from, int to, float amount) {
        float clamped = Math.clamp(amount, 0.0F, 1.0F);
        int a = mixChannel((from >>> 24) & 0xFF, (to >>> 24) & 0xFF, clamped);
        int r = mixChannel((from >>> 16) & 0xFF, (to >>> 16) & 0xFF, clamped);
        int g = mixChannel((from >>> 8) & 0xFF, (to >>> 8) & 0xFF, clamped);
        int b = mixChannel(from & 0xFF, to & 0xFF, clamped);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int mixChannel(int from, int to, float amount) {
        return Math.clamp((int) (from + ((to - from) * amount)), 0, 255);
    }

    static UIElement spacer() {
        UIElement spacer = new UIElement();
        spacer.getLayout().flex(1);
        return spacer;
    }

    record TabTheme(
            int backdropFill,
            int panelFill,
            int panelBorder,
            int accent,
            int primaryText,
            int priceText,
            int secondaryText,
            int alertText,
            int panelEdge,
            int rowHover
    ) {
        int shellFill() {
            return mixColor(panelFill, panelEdge, 0.55F);
        }

        int shellBorder() {
            return mixColor(panelBorder, panelEdge, 0.45F);
        }

        int headerFill() {
            return mixColor(panelFill, accent, 0.10F);
        }

        int sectionFill() {
            return mixColor(panelFill, panelEdge, 0.18F);
        }

        int railFill() {
            return mixColor(sectionFill(), accent, 0.08F);
        }

        int insetFill() {
            return mixColor(panelEdge, panelFill, 0.28F);
        }

        int accentSoftFill() {
            return mixColor(sectionFill(), accent, 0.12F);
        }

        int accentFill() {
            return mixColor(sectionFill(), accent, 0.24F);
        }

        int cardFill() {
            return mixColor(sectionFill(), rowHover, 0.42F);
        }

        int cardHoverFill() {
            return mixColor(cardFill(), accent, 0.10F);
        }

        int cardSelectedFill() {
            return mixColor(sectionFill(), accent, 0.34F);
        }

        int buttonFill() {
            return mixColor(sectionFill(), accent, 0.24F);
        }

        int buttonHoverFill() {
            return mixColor(buttonFill(), primaryText, 0.08F);
        }

        int buttonPressedFill() {
            return mixColor(panelEdge, accent, 0.18F);
        }

        int divider() {
            return mixColor(panelBorder, panelEdge, 0.56F);
        }

        int accentDivider() {
            return mixColor(accent, primaryText, 0.18F);
        }

        int tabFill() {
            return mixColor(shellFill(), sectionFill(), 0.52F);
        }

        int tabHoverFill() {
            return mixColor(tabFill(), accent, 0.10F);
        }

        int tabActiveFill() {
            return mixColor(headerFill(), accent, 0.22F);
        }
    }
}
