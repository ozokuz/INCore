package io.github.ozokuz.incore.features.research.material;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResearchMaterialManager extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ResearchMaterialDefinition> materials = Map.of();

    public ResearchMaterialManager() {
        super(new Gson(), "research_materials");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ResearchMaterialDefinition> next = new LinkedHashMap<>();
        Map<ResourceLocation, ResourceLocation> itemToMaterial = new LinkedHashMap<>();
        jsons.forEach((id, json) -> {
            if (!json.isJsonObject()) {
                return;
            }
            ResearchMaterialDefinition definition = readDefinition(id, json.getAsJsonObject());
            if (definition != null) {
                ResourceLocation existing = itemToMaterial.putIfAbsent(definition.itemId(), id);
                if (existing != null) {
                    INCore.LOGGER.warn("Ignoring duplicate research material '{}' for item '{}'; already mapped by '{}'.", id, definition.itemId(), existing);
                    return;
                }
                next.put(id, definition);
            }
        });

        ResearchMaterialKubeJsBridge.collect().forEach((id, definition) -> {
            ResourceLocation existing = itemToMaterial.putIfAbsent(definition.itemId(), id);
            if (existing != null) {
                INCore.LOGGER.warn("Ignoring duplicate KubeJS research material '{}' for item '{}'; already mapped by '{}'.", id, definition.itemId(), existing);
                return;
            }
            next.put(id, definition);
        });
        materials = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} research materials.", materials.size());
    }

    private static ResearchMaterialDefinition readDefinition(ResourceLocation id, JsonObject json) {
        ResourceLocation itemId = json.has("item") ? ResourceLocation.tryParse(json.get("item").getAsString()) : null;
        if (itemId == null) {
            return null;
        }

        int color = 0xFFFFFFFF;
        if (json.has("color")) {
            color = parseColor(json.get("color"));
        }
        return new ResearchMaterialDefinition(id, itemId, color);
    }

    private static int parseColor(JsonElement raw) {
        if (raw == null || raw.isJsonNull()) {
            return 0xFFFFFFFF;
        }
        try {
            if (raw.getAsJsonPrimitive().isNumber()) {
                int value = raw.getAsInt();
                return (value & 0xFF000000) == 0 ? (0xFF000000 | value) : value;
            }
            String value = raw.getAsString().trim();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            int parsed = (int) Long.parseLong(value, 16);
            if (value.length() <= 6) {
                parsed |= 0xFF000000;
            }
            return parsed;
        } catch (Throwable ignored) {
            return 0xFFFFFFFF;
        }
    }

    public static Map<ResourceLocation, ResearchMaterialDefinition> all() {
        return materials;
    }

    public static ResearchMaterialDefinition get(ResourceLocation id) {
        return id == null ? null : materials.get(id);
    }
}
