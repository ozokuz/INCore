package io.github.ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public record CardSetData(
        ResourceLocation id,
        String name,
        boolean base,
        @Nullable Instant startsAt,
        @Nullable Instant endsAt,
        int weight
) {
    public static @Nullable CardSetData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name")) {
            return null;
        }

        String name = GsonHelper.getAsString(json, "name");
        boolean base = GsonHelper.getAsBoolean(json, "base", false);
        Instant startsAt = parseInstant(GsonHelper.getAsString(json, "start", ""));
        Instant endsAt = parseInstant(GsonHelper.getAsString(json, "end", ""));
        int weight = Math.max(1, GsonHelper.getAsInt(json, "weight", 1));
        return new CardSetData(id, name, base, startsAt, endsAt, weight);
    }

    public boolean isActiveAt(Instant now) {
        if (base) {
            return true;
        }

        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }
        if (endsAt != null && !now.isBefore(endsAt)) {
            return false;
        }
        return true;
    }

    private static @Nullable Instant parseInstant(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
