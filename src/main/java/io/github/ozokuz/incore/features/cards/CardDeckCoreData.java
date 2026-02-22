package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record CardDeckCoreData(ResourceLocation id, String name, int capacityPoints, int baseIntegrity) {
    public static @Nullable CardDeckCoreData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name")) {
            return null;
        }

        return new CardDeckCoreData(
                id,
                GsonHelper.getAsString(json, "name"),
                Math.max(1, GsonHelper.getAsInt(json, "capacity_points", 10)),
                Math.max(1, GsonHelper.getAsInt(json, "base_integrity", 100))
        );
    }
}
