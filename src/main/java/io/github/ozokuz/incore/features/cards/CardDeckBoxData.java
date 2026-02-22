package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record CardDeckBoxData(ResourceLocation id, String name, int capacityBonus, int integrityBonus) {
    public static @Nullable CardDeckBoxData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name")) {
            return null;
        }

        return new CardDeckBoxData(
                id,
                GsonHelper.getAsString(json, "name"),
                Math.max(0, GsonHelper.getAsInt(json, "capacity_bonus", 0)),
                Math.max(0, GsonHelper.getAsInt(json, "integrity_bonus", 0))
        );
    }
}
