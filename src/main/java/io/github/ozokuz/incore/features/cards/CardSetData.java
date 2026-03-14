package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record CardSetData(
        ResourceLocation id,
        String name,
        boolean base,
        int weight
) {
    public static @Nullable CardSetData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name")) {
            return null;
        }

        String name = GsonHelper.getAsString(json, "name");
        boolean base = GsonHelper.getAsBoolean(json, "base", false);
        int weight = Math.max(1, GsonHelper.getAsInt(json, "weight", 1));
        return new CardSetData(id, name, base, weight);
    }
}
