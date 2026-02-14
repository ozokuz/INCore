package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class AltarOfferingManager extends SimpleJsonResourceReloadListener {
    public static final Map<ResourceLocation, AltarOfferingData> OFFERINGS = new LinkedHashMap<>();

    public AltarOfferingManager() {
        super(new Gson(), "roguelike/offerings");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        OFFERINGS.clear();

        jsons.forEach((id, json) -> {
            try {
                OFFERINGS.put(id, AltarOfferingData.fromJson(json.getAsJsonObject()));
            } catch (Exception e) {
                INCore.LOGGER.warn("Failed to load roguelike offering {}", id, e);
            }
        });

        INCore.LOGGER.info("Loaded {} roguelike altar offerings", OFFERINGS.size());
    }

    public static Optional<PickedOffering> pickRandom(RandomSource random) {
        if (OFFERINGS.isEmpty()) {
            return Optional.empty();
        }

        int totalWeight = OFFERINGS.values().stream().mapToInt(AltarOfferingData::weight).sum();
        if (totalWeight <= 0) {
            return Optional.empty();
        }

        int roll = random.nextInt(totalWeight);
        for (Map.Entry<ResourceLocation, AltarOfferingData> entry : OFFERINGS.entrySet()) {
            roll -= entry.getValue().weight();
            if (roll < 0) {
                return Optional.of(new PickedOffering(entry.getKey(), entry.getValue()));
            }
        }

        Map.Entry<ResourceLocation, AltarOfferingData> fallback = OFFERINGS.entrySet().iterator().next();
        return Optional.of(new PickedOffering(fallback.getKey(), fallback.getValue()));
    }

    public record PickedOffering(ResourceLocation id, AltarOfferingData data) {
    }
}
