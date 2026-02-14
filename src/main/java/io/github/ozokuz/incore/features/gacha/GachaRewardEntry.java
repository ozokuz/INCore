package io.github.ozokuz.incore.features.gacha;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public record GachaRewardEntry(
        ResourceLocation itemId,
        int weight,
        int rarity,
        int minCount,
        int maxCount
) {
    @Nullable
    public static GachaRewardEntry fromJson(JsonObject json) {
        if (!json.has("item") || !json.has("weight") || !json.has("rarity")) {
            return null;
        }

        ResourceLocation itemId = ResourceLocation.tryParse(json.get("item").getAsString());
        if (itemId == null) {
            return null;
        }

        int weight = Math.max(0, json.get("weight").getAsInt());
        int rarity = Math.clamp(json.get("rarity").getAsInt(), 2, 6);
        int minCount = json.has("min_count") ? Math.max(1, json.get("min_count").getAsInt()) : 1;
        int maxCount = json.has("max_count") ? Math.max(1, json.get("max_count").getAsInt()) : minCount;
        if (maxCount < minCount) {
            maxCount = minCount;
        }

        if (weight <= 0) {
            return null;
        }

        return new GachaRewardEntry(itemId, weight, rarity, minCount, maxCount);
    }

    public ItemStack createStack(RandomSource random) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        int count = minCount == maxCount ? minCount : Mth.nextInt(random, minCount, maxCount);
        return new ItemStack(item, count);
    }
}
