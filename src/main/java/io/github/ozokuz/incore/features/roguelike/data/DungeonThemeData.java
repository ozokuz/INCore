package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

public record DungeonThemeData(
        DungeonThemeData.StructureRef startingRoomStructure,
        int weight
) {
    public static DungeonThemeData fromJson(JsonObject json) {
        StructureRef startingRoom = requiredStructureRef(json, "starting_room");
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        return new DungeonThemeData(startingRoom, Math.max(1, weight));
    }

    private static StructureRef requiredStructureRef(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("Theme is missing required field '" + key + "'");
        }

        return readStructureRef(json.get(key));
    }

    private static StructureRef readStructureRef(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return new StructureRef(ResourceLocation.parse(element.getAsString()), 0);
        }

        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Structure reference must be a string id or an object");
        }

        JsonObject object = element.getAsJsonObject();
        if (!object.has("id")) {
            throw new IllegalArgumentException("Structure reference object is missing required field 'id'");
        }

        ResourceLocation id = ResourceLocation.parse(object.get("id").getAsString());
        int floorYFromBottom = object.has("floor_y_from_bottom") ? object.get("floor_y_from_bottom").getAsInt() : 0;
        return new StructureRef(id, Math.max(0, floorYFromBottom));
    }

    public record StructureRef(ResourceLocation id, int floorYFromBottom) {
    }
}
