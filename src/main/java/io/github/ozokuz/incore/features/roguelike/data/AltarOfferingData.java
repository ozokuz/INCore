package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public record AltarOfferingData(Item item, int baseCost, int rampEvery, int weight) {
    public static AltarOfferingData fromJson(JsonObject json) {
        ResourceLocation itemId = ResourceLocation.parse(json.get("item").getAsString());
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("Unknown item in roguelike offering: " + itemId);
        }

        int baseCost = json.has("base_cost") ? json.get("base_cost").getAsInt() : 4;
        int rampEvery = json.has("ramp_every") ? json.get("ramp_every").getAsInt() : 3;
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;

        return new AltarOfferingData(item, Math.max(1, baseCost), Math.max(1, rampEvery), Math.max(1, weight));
    }

    public int requiredAmount(int crystalsCrafted) {
        return Math.max(1, baseCost + Math.max(0, crystalsCrafted) / rampEvery);
    }
}
