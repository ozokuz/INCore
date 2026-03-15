package ozokuz.incore.features.cards;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CardDeckBoxManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, CardDeckBoxData> boxes = Map.of();

    public CardDeckBoxManager() {
        super(new Gson(), "deck_boxes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CardDeckBoxData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    CardDeckBoxData data = CardDeckBoxData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (data != null) {
                        next.put(entry.getKey(), data);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid deck box '{}'", entry.getKey());
                    }
                });
        boxes = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} deck box definitions.", boxes.size());
    }

    public static CardDeckBoxData get(ResourceLocation id) {
        return boxes.get(id);
    }

    public static List<CardDeckBoxData> all() {
        return boxes.values().stream().toList();
    }

    public static ResourceLocation getDefaultBoxId() {
        return boxes.keySet().stream().findFirst().orElse(ResourceLocation.parse("incore:starter_box"));
    }
}
