package ozokuz.incore.features.arena.data;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record ArenaCatalogEntry(
        ResourceLocation id,
        String categoryId,
        String categoryName,
        String difficultyId,
        String difficultyName,
        ResourceLocation gatewayId,
        int rewardEntropyCost,
        List<ArenaRewardStack> rewardItems,
        String rewardSummary,
        int sortOrder
) {
    @Nullable
    public static ArenaCatalogEntry fromJson(ResourceLocation id, JsonObject json) {
        if (!json.has("category_id")
                || !json.has("category_name")
                || !json.has("difficulty_id")
                || !json.has("difficulty_name")
                || !json.has("gateway_id")
                || !json.has("reward")) {
            return null;
        }

        JsonObject reward = json.getAsJsonObject("reward");
        if (!reward.has("entropy_cost") || !reward.has("items")) {
            return null;
        }

        ResourceLocation gatewayId = ResourceLocation.tryParse(json.get("gateway_id").getAsString());
        if (gatewayId == null) {
            return null;
        }

        List<ArenaRewardStack> rewardItems = new ArrayList<>();
        for (var rewardElement : reward.getAsJsonArray("items")) {
            if (!rewardElement.isJsonObject()) {
                continue;
            }

            ArenaRewardStack stack = ArenaRewardStack.fromJson(rewardElement.getAsJsonObject());
            if (stack != null) {
                rewardItems.add(stack);
            }
        }
        if (rewardItems.isEmpty()) {
            return null;
        }

        int entropyCost = Math.max(0, reward.get("entropy_cost").getAsInt());
        String summary = json.has("reward_summary") ? json.get("reward_summary").getAsString() : "";
        int sortOrder = json.has("sort_order") ? json.get("sort_order").getAsInt() : 0;

        return new ArenaCatalogEntry(
                id,
                json.get("category_id").getAsString(),
                json.get("category_name").getAsString(),
                json.get("difficulty_id").getAsString(),
                json.get("difficulty_name").getAsString(),
                gatewayId,
                entropyCost,
                List.copyOf(rewardItems),
                summary,
                sortOrder
        );
    }
}
