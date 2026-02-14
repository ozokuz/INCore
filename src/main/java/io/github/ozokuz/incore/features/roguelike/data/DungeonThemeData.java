package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public record DungeonThemeData(Block floorBlock, Block wallBlock, Block ceilingBlock, int weight) {
    public static DungeonThemeData fromJson(JsonObject json) {
        Block floor = blockFromJson(json, "floor", Blocks.STONE);
        Block wall = blockFromJson(json, "wall", Blocks.STONE_BRICKS);
        Block ceiling = blockFromJson(json, "ceiling", wall);
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;

        return new DungeonThemeData(floor, wall, ceiling, Math.max(1, weight));
    }

    private static Block blockFromJson(JsonObject json, String key, Block fallback) {
        if (!json.has(key)) {
            return fallback;
        }

        ResourceLocation id = ResourceLocation.parse(json.get(key).getAsString());
        Block block = BuiltInRegistries.BLOCK.get(id);
        return block == Blocks.AIR ? fallback : block;
    }
}
