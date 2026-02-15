package io.github.ozokuz.incore.features.research;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class LabProcessManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, LabProcessData> processes = Map.of();

    public LabProcessManager() {
        super(new Gson(), "lab_processes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, LabProcessData> next = new LinkedHashMap<>();
        jsons.forEach((id, json) -> {
            if (json.isJsonObject()) {
                next.put(id, LabProcessData.fromJson(id, json.getAsJsonObject()));
            }
        });
        processes = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} lab processes.", processes.size());
    }

    public static Map<ResourceLocation, LabProcessData> all() {
        return processes;
    }

    public static LabProcessData match(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return processes.values().stream()
                .filter(process -> itemId.equals(process.itemId()) && stack.getCount() >= process.itemCount())
                .findFirst()
                .orElse(null);
    }
}
