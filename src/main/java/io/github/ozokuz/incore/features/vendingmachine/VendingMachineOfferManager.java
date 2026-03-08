package io.github.ozokuz.incore.features.vendingmachine;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
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

public class VendingMachineOfferManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, VendingMachineOfferData> offers = Map.of();

    public VendingMachineOfferManager() {
        super(new Gson(), "vending_machine/offers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, VendingMachineOfferData> next = new LinkedHashMap<>();
        jsons.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    if (!entry.getValue().isJsonObject()) {
                        return;
                    }

                    VendingMachineOfferData data = VendingMachineOfferData.fromJson(entry.getKey(), entry.getValue().getAsJsonObject());
                    if (data != null) {
                        next.put(entry.getKey(), data);
                    } else {
                        INCore.LOGGER.warn("Skipping invalid vending machine offer '{}'", entry.getKey());
                    }
                });

        offers = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} vending machine offers.", offers.size());
    }

    public static List<VendingMachineOfferData> all() {
        return offers.values().stream().toList();
    }

    public static @Nullable VendingMachineOfferData get(ResourceLocation id) {
        return offers.get(id);
    }
}
