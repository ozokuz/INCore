package io.github.ozokuz.incore.features.cards;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CardModuleManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, CardModuleData> modules = Map.of();

    public CardModuleManager() {
        super(new Gson(), "cards");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CardModuleData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    CardModuleData module = CardModuleData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (module != null) {
                        next.put(entry.getKey(), module);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid card module '{}'", entry.getKey());
                    }
                });
        modules = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} card module definitions.", modules.size());
    }

    public static CardModuleData get(ResourceLocation id) {
        return modules.get(id);
    }

    public static List<CardModuleData> all() {
        return modules.values().stream().toList();
    }

    public static List<CardModuleData> bySet(ResourceLocation setId) {
        return modules.values().stream().filter(module -> module.setId().equals(setId)).toList();
    }
}
