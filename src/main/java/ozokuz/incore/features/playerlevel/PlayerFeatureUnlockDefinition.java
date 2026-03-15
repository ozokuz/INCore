package ozokuz.incore.features.playerlevel;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

public record PlayerFeatureUnlockDefinition(
        ResourceLocation id,
        int requiredLevel,
        ResourceLocation iconItemId,
        String displayName,
        @Nullable String description
) {
    public static PlayerFeatureUnlockDefinition fromJson(JsonObject jsonObject) {
        ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "id"));
        int requiredLevel = Math.max(1, GsonHelper.getAsInt(jsonObject, "required_level"));
        ResourceLocation iconItemId = ResourceLocation.parse(GsonHelper.getAsString(jsonObject, "icon_item"));
        String displayName = GsonHelper.getAsString(jsonObject, "display_name");
        String description = GsonHelper.getAsString(jsonObject, "description", "");
        return new PlayerFeatureUnlockDefinition(id, requiredLevel, iconItemId, displayName, description.isBlank() ? null : description);
    }
}
