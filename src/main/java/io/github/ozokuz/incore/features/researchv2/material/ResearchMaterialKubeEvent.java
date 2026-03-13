package io.github.ozokuz.incore.features.researchv2.material;

import dev.latvian.mods.kubejs.event.KubeStartupEvent;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResearchMaterialKubeEvent implements KubeStartupEvent {
    private final Map<ResourceLocation, ResearchMaterialDefinition> additions = new LinkedHashMap<>();

    public void add(String id, String item, String color) {
        ResourceLocation materialId = ResourceLocation.tryParse(id);
        ResourceLocation itemId = ResourceLocation.tryParse(item);
        if (materialId == null || itemId == null) {
            return;
        }
        int parsedColor = parseColor(color);
        additions.put(materialId, new ResearchMaterialDefinition(materialId, itemId, parsedColor));
    }

    public void add(String id, String item, int color) {
        ResourceLocation materialId = ResourceLocation.tryParse(id);
        ResourceLocation itemId = ResourceLocation.tryParse(item);
        if (materialId == null || itemId == null) {
            return;
        }
        int parsedColor = (color & 0xFF000000) == 0 ? (0xFF000000 | color) : color;
        additions.put(materialId, new ResearchMaterialDefinition(materialId, itemId, parsedColor));
    }

    public Map<ResourceLocation, ResearchMaterialDefinition> additions() {
        return additions;
    }

    private static int parseColor(String rawColor) {
        if (rawColor == null || rawColor.isBlank()) {
            return 0xFFFFFFFF;
        }
        try {
            String value = rawColor.trim();
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
}
