package ozokuz.incore.features.gacha;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GachaEventCategoryManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, GachaEventCategoryData> categories = Map.of();

    public GachaEventCategoryManager() {
        super(new Gson(), "gacha_event_categories");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, GachaEventCategoryData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation id = entry.getKey();
                    JsonElement json = entry.getValue();
                    if (!json.isJsonObject()) {
                        return;
                    }
                    GachaEventCategoryData category = GachaEventCategoryData.fromJson(id, json.getAsJsonObject());
                    if (category != null) {
                        next.put(id, category);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid gacha event category '{}'", id);
                    }
                });
        categories = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} gacha event categories.", categories.size());
    }

    public static List<GachaEventCategoryData> all() {
        return categories.values().stream().toList();
    }

    public static @Nullable GachaEventCategoryData get(ResourceLocation id) {
        return categories.get(id);
    }
}
