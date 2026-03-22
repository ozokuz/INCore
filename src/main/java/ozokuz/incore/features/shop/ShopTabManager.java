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
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.INCore;

public final class ShopTabManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ShopTabId, ShopTabDefinition> byId = Map.of();
    private static volatile Map<ResourceLocation, ShopTabId> byCategoryId = Map.of();
    private static volatile List<ShopTabDefinition> ordered = List.of();

    public ShopTabManager() {
        super(new Gson(), "shop_tabs");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ShopTabId, ShopTabDefinition> next = new LinkedHashMap<>();
        Map<ResourceLocation, ShopTabId> categoryOwners = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    try {
                        if (!entry.getValue().isJsonObject()) {
                            return;
                        }
                        ShopTabId tabId = ShopTabId.tryParse(entry.getKey().getPath());
                        if (tabId == null) {
                            throw new IllegalArgumentException("Unknown shop tab id " + entry.getKey());
                        }
                        ShopTabDefinition definition = ShopTabDefinition.fromJson(
                                tabId,
                                entry.getValue().getAsJsonObject(),
                                categoryId -> ShopCategoryManager.get(categoryId) != null
                        );
                        for (ResourceLocation categoryId : definition.categoryIds()) {
                            ShopTabId previous = categoryOwners.putIfAbsent(categoryId, tabId);
                            if (previous != null) {
                                throw new IllegalArgumentException("Category " + categoryId + " is already owned by " + previous.serialized());
                            }
                        }
                        next.put(tabId, definition);
                    } catch (Exception exception) {
                        INCore.LOGGER.error("Failed to parse shop tab {}", entry.getKey(), exception);
                    }
                });

        List<ShopTabDefinition> sorted = new ArrayList<>();
        for (ShopTabId tabId : ShopTabId.values()) {
            ShopTabDefinition definition = next.get(tabId);
            if (definition != null) {
                sorted.add(definition);
            }
        }

        byId = Map.copyOf(next);
        byCategoryId = Map.copyOf(categoryOwners);
        ordered = List.copyOf(sorted);
        INCore.LOGGER.info("Loaded {} shop tab definition(s)", ordered.size());
    }

    public static List<ShopTabDefinition> all() {
        return ordered;
    }

    public static @Nullable ShopTabDefinition get(ShopTabId id) {
        return byId.get(id);
    }

    public static @Nullable ShopTabId tabForCategory(ResourceLocation categoryId) {
        return byCategoryId.get(categoryId);
    }
}
