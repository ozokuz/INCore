package io.github.ozokuz.incore.features.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Set;

public record ResearchRecipeLockSetData(
        ResourceLocation id,
        Set<ResourceLocation> recipes,
        Set<ResourceLocation> recipeTags
) {
    public static ResearchRecipeLockSetData fromJson(ResourceLocation id, JsonObject json) {
        return new ResearchRecipeLockSetData(
                id,
                Set.copyOf(readIds(json.getAsJsonArray("recipes"))),
                Set.copyOf(readIds(json.getAsJsonArray("recipe_tags")))
        );
    }

    private static Set<ResourceLocation> readIds(JsonArray array) {
        Set<ResourceLocation> ids = new LinkedHashSet<>();
        if (array == null) {
            return ids;
        }
        for (JsonElement element : array) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return ids;
    }
}

