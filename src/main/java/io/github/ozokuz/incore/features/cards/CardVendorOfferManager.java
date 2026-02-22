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

public class CardVendorOfferManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, CardVendorOfferData> offers = Map.of();

    public CardVendorOfferManager() {
        super(new Gson(), "vendor_offers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, CardVendorOfferData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    CardVendorOfferData data = CardVendorOfferData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (data != null) {
                        next.put(entry.getKey(), data);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid vendor offer '{}'", entry.getKey());
                    }
                });

        offers = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} card vendor offers.", offers.size());
    }

    public static List<CardVendorOfferData> all() {
        return offers.values().stream().toList();
    }

    public static CardVendorOfferData get(ResourceLocation id) {
        return offers.get(id);
    }
}
