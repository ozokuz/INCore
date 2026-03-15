package ozokuz.incore.features.cards;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record CardBoosterBoxData(ResourceLocation id, String name, ResourceLocation setId, int boosterCount) {
    public static @Nullable CardBoosterBoxData fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("name")) {
            return null;
        }

        ResourceLocation configuredSet = ResourceLocation.tryParse(GsonHelper.getAsString(json, "set", ""));
        ResourceLocation configuredBooster = ResourceLocation.tryParse(GsonHelper.getAsString(json, "booster", ""));
        ResourceLocation resolvedSetId = configuredSet;
        if (resolvedSetId == null && configuredBooster != null) {
            resolvedSetId = CardBoosterManager.resolveSetId(configuredBooster);
        }

        if (resolvedSetId == null || CardBoosterManager.get(resolvedSetId) == null) {
            return null;
        }

        return new CardBoosterBoxData(
                id,
                GsonHelper.getAsString(json, "name"),
                resolvedSetId,
                Math.max(1, GsonHelper.getAsInt(json, "booster_count", 5))
        );
    }
}
