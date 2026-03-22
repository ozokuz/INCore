package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

public record ShopTabDefinition(
        ShopTabId id,
        String displayName,
        ShopPaletteId paletteId,
        ShopLayoutId layoutId,
        ShopCategoryNavigationMode categoryNavigationMode,
        ShopDetailsPresentationMode detailsMode,
        List<ResourceLocation> categoryIds,
        ShopShowcaseDefinition showcase
) {
    public static ShopTabDefinition fromJson(
            ShopTabId id,
            JsonObject object,
            Predicate<ResourceLocation> categoryExists
    ) {
        String displayName = GsonHelper.getAsString(object, "display_name", id.displayName().getString());
        ShopPaletteId paletteId = ShopPaletteId.fromString(GsonHelper.getAsString(object, "palette", ShopPaletteId.TACTICAL_ARCHIVE.serialized()));
        ShopLayoutId layoutId = ShopLayoutId.fromString(GsonHelper.getAsString(object, "layout", id.serialized()));
        ShopCategoryNavigationMode categoryNavigationMode = ShopCategoryNavigationMode.fromString(
                GsonHelper.getAsString(object, "category_navigation", ShopCategoryNavigationMode.SIDEBAR.serialized())
        );
        ShopDetailsPresentationMode detailsMode = ShopDetailsPresentationMode.fromString(
                GsonHelper.getAsString(object, "details_mode", ShopDetailsPresentationMode.INLINE_DOCK.serialized())
        );

        List<ResourceLocation> categoryIds = new ArrayList<>();
        for (var entry : GsonHelper.getAsJsonArray(object, "categories")) {
            ResourceLocation categoryId = ResourceLocation.parse(entry.getAsString());
            if (!categoryExists.test(categoryId)) {
                throw new IllegalArgumentException("Unknown shop category " + categoryId + " referenced by " + id.serialized());
            }
            if (categoryIds.contains(categoryId)) {
                throw new IllegalArgumentException("Duplicate shop category " + categoryId + " in " + id.serialized());
            }
            categoryIds.add(categoryId);
        }

        ShopShowcaseDefinition showcase = object.has("showcase")
                ? ShopShowcaseDefinition.fromJson(GsonHelper.getAsJsonObject(object, "showcase"))
                : ShopShowcaseDefinition.disabled();

        for (ResourceLocation categoryId : showcase.categoryScope()) {
            if (!categoryIds.contains(categoryId)) {
                throw new IllegalArgumentException("Showcase category scope " + categoryId + " is not part of " + id.serialized());
            }
        }

        return new ShopTabDefinition(
                id,
                displayName,
                paletteId,
                layoutId,
                categoryNavigationMode,
                detailsMode,
                List.copyOf(categoryIds),
                showcase
        );
    }
}
