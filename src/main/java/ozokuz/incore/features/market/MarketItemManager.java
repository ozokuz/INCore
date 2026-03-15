package ozokuz.incore.features.market;

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

public final class MarketItemManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, MarketItemDefinition> byItemId = Map.of();
    private static volatile List<MarketItemDefinition> ordered = List.of();

    public MarketItemManager() {
        super(new Gson(), "market_items");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, MarketItemDefinition> next = new LinkedHashMap<>();
        jsons.forEach((id, json) -> {
            try {
                if (!json.isJsonObject()) {
                    return;
                }
                MarketItemDefinition definition = MarketItemDefinition.fromJson(id, json.getAsJsonObject());
                if (!definition.enabled()) {
                    return;
                }
                if (BuiltInRegistries.ITEM.get(definition.itemId()) == Items.AIR) {
                    INCore.LOGGER.warn("Skipping market item {} due to unknown item {}", id, definition.itemId());
                    return;
                }
                next.put(definition.itemId(), definition);
            } catch (Exception exception) {
                INCore.LOGGER.error("Failed to parse market item {}", id, exception);
            }
        });

        List<MarketItemDefinition> sorted = new ArrayList<>(next.values());
        sorted.sort(Comparator
                .comparingInt(MarketItemDefinition::sortOrder)
                .thenComparing(MarketItemDefinition::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(definition -> definition.itemId().toString()));

        byItemId = Map.copyOf(next);
        ordered = List.copyOf(sorted);

        INCore.LOGGER.info("Loaded {} market item(s)", ordered.size());
    }

    public static List<MarketItemDefinition> all() {
        return ordered;
    }

    public static MarketItemDefinition get(ResourceLocation itemId) {
        return byItemId.get(itemId);
    }

    public static boolean isTradeable(ResourceLocation itemId) {
        return byItemId.containsKey(itemId);
    }
}
