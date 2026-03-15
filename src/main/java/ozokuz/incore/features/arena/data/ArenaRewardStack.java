package ozokuz.incore.features.arena.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

public record ArenaRewardStack(ResourceLocation itemId, int count) {
    @Nullable
    public static ArenaRewardStack fromJson(JsonObject json) {
        if (!json.has("item")) {
            return null;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(json.get("item").getAsString());
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return null;
        }

        int count = json.has("count") ? json.get("count").getAsInt() : 1;
        if (count <= 0) {
            return null;
        }

        return new ArenaRewardStack(itemId, count);
    }

    @Nullable
    public Item resolveItem() {
        return BuiltInRegistries.ITEM.get(itemId);
    }
}
