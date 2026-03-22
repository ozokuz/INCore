package ozokuz.incore.features.shop;

import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

public record ShopCategoryRotationDefinition(int durationHours, int visibleCount) {
    public static ShopCategoryRotationDefinition fromJson(JsonObject object) {
        return new ShopCategoryRotationDefinition(
                Math.max(1, GsonHelper.getAsInt(object, "duration_hours", 24)),
                Math.max(1, GsonHelper.getAsInt(object, "visible_count", 1))
        );
    }
}
