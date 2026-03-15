package ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record CardBoosterData(
        ResourceLocation id,
        String name,
        ResourceLocation setId,
        int cardsPerPack,
        double foilChance
) {
    public static @Nullable CardBoosterData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name") || !json.has("set")) {
            return null;
        }

        ResourceLocation setId = ResourceLocation.tryParse(GsonHelper.getAsString(json, "set", ""));
        if (setId == null) {
            return null;
        }

        return new CardBoosterData(
                id,
                GsonHelper.getAsString(json, "name"),
                setId,
                Math.max(1, GsonHelper.getAsInt(json, "cards_per_pack", 5)),
                Math.clamp(GsonHelper.getAsDouble(json, "foil_chance", 0.08D), 0.0D, 1.0D)
        );
    }
}
