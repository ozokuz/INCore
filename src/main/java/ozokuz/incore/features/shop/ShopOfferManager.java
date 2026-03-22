package ozokuz.incore.features.shop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import ozokuz.incore.INCore;

public final class ShopOfferManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ShopOfferDefinition> byId = Map.of();
    private static volatile List<ShopOfferDefinition> ordered = List.of();

    public ShopOfferManager() {
        super(new Gson(), "shop_offers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ShopOfferDefinition> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = entry.getKey();
                    JsonElement json = entry.getValue();
                    try {
                        if (!json.isJsonObject()) {
                            return;
                        }

                        ShopOfferDefinition offer = ShopOfferDefinition.fromJson(id, json.getAsJsonObject());
                        if (ShopCategoryManager.get(offer.categoryId()) == null) {
                            INCore.LOGGER.warn("Skipping shop offer {} due to unknown category {}", id, offer.categoryId());
                            return;
                        }
                        if (!isValidPurchaseable(offer.purchaseable())) {
                            INCore.LOGGER.warn("Skipping shop offer {} due to invalid reward stack(s)", id);
                            return;
                        }

                        next.put(offer.id(), offer);
                    } catch (Exception exception) {
                        INCore.LOGGER.error("Failed to parse shop offer {}", id, exception);
                    }
                });

        List<ShopOfferDefinition> sorted = new ArrayList<>(next.values());
        sorted.sort(Comparator
                .comparing((ShopOfferDefinition offer) -> {
                    ShopCategoryDefinition category = ShopCategoryManager.get(offer.categoryId());
                    return category == null ? Integer.MAX_VALUE : category.sortOrder();
                })
                .thenComparing(offer -> offer.id().toString()));

        byId = Map.copyOf(next);
        ordered = List.copyOf(sorted);
        INCore.LOGGER.info("Loaded {} shop offer definition(s)", ordered.size());
    }

    public static List<ShopOfferDefinition> all() {
        return ordered;
    }

    public static List<ShopOfferDefinition> byCategory(ResourceLocation categoryId) {
        return ordered.stream()
                .filter(offer -> offer.categoryId().equals(categoryId))
                .sorted(Comparator.comparing(offer -> offer.id().toString()))
                .toList();
    }

    public static ShopOfferDefinition get(ResourceLocation id) {
        return byId.get(id);
    }

    public static List<ResourceLocation> ids() {
        return ordered.stream().map(ShopOfferDefinition::id).toList();
    }

    private static boolean isValidPurchaseable(ShopPurchaseableDefinition purchaseable) {
        return switch (purchaseable) {
            case ShopSingleItemPurchaseableDefinition singleItem -> ShopService.parseStack(singleItem.stackSpec(), singleItem.count()) != null;
            case ShopBundlePurchaseableDefinition bundle -> bundle.items().stream()
                    .allMatch(entry -> ShopService.parseStack(entry.stackSpec(), entry.count()) != null);
        };
    }
}
