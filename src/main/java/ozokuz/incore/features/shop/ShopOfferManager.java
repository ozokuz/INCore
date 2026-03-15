package ozokuz.incore.features.shop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import ozokuz.incore.INCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                        if (!offer.enabled()) {
                            return;
                        }
                        if (ShopCategoryManager.get(offer.categoryId()) == null) {
                            INCore.LOGGER.warn("Skipping shop offer {} due to unknown category {}", id, offer.categoryId());
                            return;
                        }
                        if (BuiltInRegistries.ITEM.get(offer.itemId()) == Items.AIR) {
                            INCore.LOGGER.warn("Skipping shop offer {} due to unknown item {}", id, offer.itemId());
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
                .thenComparingInt(ShopOfferDefinition::sortOrder)
                .thenComparing(ShopOfferDefinition::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(offer -> offer.id().toString()));

        byId = Map.copyOf(next);
        ordered = List.copyOf(sorted);
        INCore.LOGGER.info("Loaded {} shop offer definition(s)", ordered.size());
    }

    public static List<ShopOfferDefinition> all() {
        return ordered;
    }

    public static List<ShopOfferDefinition> byCategory(ResourceLocation categoryId) {
        return ordered.stream().filter(offer -> offer.categoryId().equals(categoryId)).toList();
    }

    public static ShopOfferDefinition get(ResourceLocation id) {
        return byId.get(id);
    }

    public static List<ResourceLocation> ids() {
        return ordered.stream().map(ShopOfferDefinition::id).toList();
    }
}
