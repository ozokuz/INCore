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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
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
        return new ShopService.ScreenData(0, "", "", List.of(), List.of());
    }

    static ShopService.ScreenData parse(String json) {
        ShopService.ScreenData parsed = GSON.fromJson(json, ShopService.ScreenData.class);
        if (parsed == null || parsed.categories() == null || parsed.offers() == null) {
            return emptyData();
        }
        return new ShopService.ScreenData(
                parsed.balanceSpur(),
                parsed.selectedCategoryId() == null ? "" : parsed.selectedCategoryId(),
                parsed.selectedOfferId() == null ? "" : parsed.selectedOfferId(),
                parsed.categories(),
                parsed.offers()
        );
    }

    static List<ShopService.CategoryView> orderedCategories(ShopService.ScreenData data) {
        List<ShopService.CategoryView> copy = new ArrayList<>(data.categories());
        copy.sort(Comparator
                .comparingInt(ShopService.CategoryView::sortOrder)
                .thenComparing(ShopService.CategoryView::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ShopService.CategoryView::categoryId));
        return copy;
    }

    static List<ShopService.CategoryView> categoriesForTab(ShopService.ScreenData data, ShopTabId tabId) {
        return orderedCategories(data).stream()
                .filter(category -> tabForCategory(category) == tabId)
                .toList();
    }

    static List<ShopService.OfferView> offersForCategory(ShopService.ScreenData data, @Nullable String categoryId) {
        List<ShopService.OfferView> filtered = new ArrayList<>();
        for (ShopService.OfferView offer : data.offers()) {
            if (categoryId != null && !categoryId.isBlank() && categoryId.equals(offer.categoryId())) {
                filtered.add(offer);
            }
        }
        filtered.sort(Comparator
                .comparingInt(ShopService.OfferView::sortOrder)
                .thenComparing(ShopService.OfferView::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ShopService.OfferView::offerId));
        return filtered;
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

    static ShopTabId tabForCategory(ShopService.CategoryView category) {
        return ShopTabId.fromString(category.tabId());
    }

    static ShopTabId tabForCategoryId(ShopService.ScreenData data, @Nullable String categoryId) {
        ShopService.CategoryView category = findCategory(data, categoryId);
        return category == null ? ShopTabId.SUPPLIES : tabForCategory(category);
    }

    static ItemStack stackForOffer(ShopService.OfferView offer) {
        ResourceLocation itemId = ResourceLocation.tryParse(offer.itemId());
        if (itemId == null) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item);
    }

    static String stockLabel(int stock) {
        return stock < 0
                ? Component.translatable("screen.incore.shop.stock.unlimited").getString()
                : Component.translatable("screen.incore.shop.stock.remaining", stock).getString();
    }

    static String bundleLabel(int count) {
        return Component.translatable("screen.incore.shop.bundle", count).getString();
    }

    static String priceLabel(int price) {
        return Component.translatable("screen.incore.shop.price_each", price).getString();
    }

    static TabTheme themeFor(ShopTabId tabId) {
        return switch (tabId) {
            case SUPPLIES -> new TabTheme(
                    0x79111210,
                    0xFF1F221F,
                    0xFF313731,
                    0xFF97A971,
                    0xFFC8D5B2,
                    0xFFB69B59,
                    0xFFAEA58D,
                    0xFF854B2E,
                    0xFF242922,
                    0xFF4C5847
            );
            case ROTATIONS -> new TabTheme(
                    0x7A080E16,
                    0xFF171E2D,
                    0xFF27344A,
                    0xFF73D7FF,
                    0xFFD8EEFF,
                    0xFFFFD36E,
                    0xFF9CB5CF,
                    0xFFB16EFF,
                    0xFF1A2234,
                    0xFF39506E
            );
            case CACHES -> new TabTheme(
                    0x7A100D14,
                    0xFF211724,
                    0xFF3D2E45,
                    0xFFCC8CFF,
                    0xFFF0DBFF,
                    0xFFFFB46D,
                    0xFFB8A6C5,
                    0xFF71C7C5,
                    0xFF241B29,
                    0xFF594669
            );
        };
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
                .baseTexture(buttonTexture(theme.panelFill(), theme.panelBorder(), theme.accent(), 1))
                .hoverTexture(buttonTexture(theme.panelBorder(), theme.accent(), theme.primaryText(), 1))
                .pressedTexture(buttonTexture(theme.panelFill(), theme.accent(), theme.primaryText(), 1))
        );
        return button;
    }

    static Button chipButton(Component text, TabTheme theme, boolean active) {
        Button button = actionButton(text, theme, 24);
        button.layout(layout -> {
            layout.minWidth(108);
            layout.paddingHorizontal(8);
        });
        if (active) {
            button.buttonStyle(style -> style
                    .baseTexture(buttonTexture(theme.accent(), theme.primaryText(), theme.priceText(), 1))
                    .hoverTexture(buttonTexture(theme.accent(), theme.primaryText(), theme.priceText(), 1))
                    .pressedTexture(buttonTexture(theme.accent(), theme.primaryText(), theme.priceText(), 1))
            );
            button.text.textStyle(style -> style.textColor(0xFF111111));
        }
        return button;
    }

    static UIElement surface(TabTheme theme, int padding, int radius) {
        UIElement element = new UIElement();
        element.layout(layout -> {
            layout.widthPercent(100);
            layout.gapAll(8);
            layout.paddingAll(padding);
        });
        element.style(style -> style.backgroundTexture(buttonTexture(theme.panelFill(), theme.panelBorder(), theme.panelEdge(), radius)));
        return element;
    }

    static Label heading(Component text, int color) {
        Label label = new Label();
        label.setText(text);
        label.getLayout().widthPercent(100);
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
        label.getLayout().widthPercent(100);
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
    }
}
