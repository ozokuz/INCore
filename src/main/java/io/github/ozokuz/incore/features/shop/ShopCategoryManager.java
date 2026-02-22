package io.github.ozokuz.incore.features.shop;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.gacha.GachaEventCategoryManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ShopCategoryManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ShopCategoryDefinition> byId = Map.of();
    private static volatile List<ShopCategoryDefinition> ordered = List.of();

    public ShopCategoryManager() {
        super(new Gson(), "shop_categories");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ShopCategoryDefinition> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = entry.getKey();
                    JsonElement json = entry.getValue();
                    try {
                        if (!json.isJsonObject()) {
                            return;
                        }

                        ShopCategoryDefinition category = ShopCategoryDefinition.fromJson(id, json.getAsJsonObject());
                        if (!category.enabled()) {
                            return;
                        }

                        if (category.replenishMode() == ShopReplenishMode.GACHA_ROTATION
                                && (category.gachaCategoryId() == null || GachaEventCategoryManager.get(category.gachaCategoryId()) == null)) {
                            INCore.LOGGER.warn(
                                    "Shop category {} uses gacha_rotation but gacha_category {} is missing.",
                                    id,
                                    category.gachaCategoryId()
                            );
                        }

                        next.put(category.id(), category);
                    } catch (Exception exception) {
                        INCore.LOGGER.error("Failed to parse shop category {}", id, exception);
                    }
                });

        List<ShopCategoryDefinition> sorted = new ArrayList<>(next.values());
        sorted.sort(Comparator
                .comparingInt(ShopCategoryDefinition::sortOrder)
                .thenComparing(ShopCategoryDefinition::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(definition -> definition.id().toString()));

        byId = Map.copyOf(next);
        ordered = List.copyOf(sorted);
        INCore.LOGGER.info("Loaded {} shop category definition(s)", ordered.size());
    }

    public static List<ShopCategoryDefinition> all() {
        return ordered;
    }

    public static ShopCategoryDefinition get(ResourceLocation id) {
        return byId.get(id);
    }

    public static List<ResourceLocation> ids() {
        return ordered.stream().map(ShopCategoryDefinition::id).toList();
    }
}
