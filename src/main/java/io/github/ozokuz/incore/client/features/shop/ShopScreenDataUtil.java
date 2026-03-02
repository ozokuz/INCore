package io.github.ozokuz.incore.client.features.shop;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.shop.ShopService;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ShopScreenDataUtil {
    private static final Gson GSON = new Gson();

    private ShopScreenDataUtil() {
    }

    static ShopService.ScreenData parse(String json) {
        ShopService.ScreenData parsed = GSON.fromJson(json, ShopService.ScreenData.class);
        if (parsed == null || parsed.categories() == null || parsed.offers() == null) {
            return new ShopService.ScreenData(0, "", "", List.of(), List.of());
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
        if (data == null || data.categories() == null || data.categories().isEmpty()) {
            return List.of();
        }

        List<ShopService.CategoryView> copy = new ArrayList<>(data.categories());
        copy.sort(Comparator
                .comparingInt(ShopService.CategoryView::sortOrder)
                .thenComparing(ShopService.CategoryView::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ShopService.CategoryView::categoryId));
        return copy;
    }

    static List<ShopService.OfferView> offersForCategory(ShopService.ScreenData data, @Nullable String categoryId) {
        if (data == null || data.offers() == null || data.offers().isEmpty()) {
            return List.of();
        }

        List<ShopService.OfferView> filtered = new ArrayList<>();
        for (ShopService.OfferView offer : data.offers()) {
            if (categoryId == null || categoryId.isBlank() || categoryId.equals(offer.categoryId())) {
                filtered.add(offer);
            }
        }
        filtered.sort(Comparator
                .comparingInt(ShopService.OfferView::sortOrder)
                .thenComparing(ShopService.OfferView::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ShopService.OfferView::offerId));
        return filtered;
    }

    static @Nullable ShopService.OfferView findOffer(ShopService.ScreenData data, @Nullable String offerId) {
        if (data == null || offerId == null || offerId.isBlank()) {
            return null;
        }

        for (ShopService.OfferView offer : data.offers()) {
            if (offerId.equals(offer.offerId())) {
                return offer;
            }
        }
        return null;
    }

    static @Nullable ShopService.CategoryView findCategory(ShopService.ScreenData data, @Nullable String categoryId) {
        if (data == null || categoryId == null || categoryId.isBlank()) {
            return null;
        }

        for (ShopService.CategoryView category : data.categories()) {
            if (categoryId.equals(category.categoryId())) {
                return category;
            }
        }
        return null;
    }

    static @Nullable ResourceLocation parseResource(@Nullable String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw);
    }
}
