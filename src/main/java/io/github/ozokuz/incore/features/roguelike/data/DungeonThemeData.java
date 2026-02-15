package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.List;

public record DungeonThemeData(
        DungeonThemeData.StructureRef startingRoomStructure,
        DungeonThemeData.StructureRef hallwayNorthSouthStructure,
        DungeonThemeData.StructureRef hallwayEastWestStructure,
        List<DungeonThemeData.RoomStructure> roomStructures,
        int weight
) {
    public static DungeonThemeData fromJson(JsonObject json) {
        StructureRef startingRoom = requiredStructureRef(json, "starting_room");
        StructureRef hallwayNs = requiredStructureRef(json, "hallway_ns");
        StructureRef hallwayEw = requiredStructureRef(json, "hallway_ew");
        List<RoomStructure> rooms = readRooms(json);
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        if (rooms.isEmpty()) {
            throw new IllegalArgumentException("Theme is missing at least one room definition in 'rooms'");
        }

        return new DungeonThemeData(startingRoom, hallwayNs, hallwayEw, rooms, Math.max(1, weight));
    }

    public RoomStructure pickRandomRoom(RandomSource random) {
        int totalWeight = 0;
        for (RoomStructure room : roomStructures) {
            totalWeight += room.weight();
        }

        int roll = random.nextInt(totalWeight);
        for (RoomStructure room : roomStructures) {
            roll -= room.weight();
            if (roll < 0) {
                return room;
            }
        }

        return roomStructures.get(0);
    }

    private static StructureRef requiredStructureRef(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("Theme is missing required field '" + key + "'");
        }

        return readStructureRef(json.get(key));
    }

    private static List<RoomStructure> readRooms(JsonObject json) {
        if (!json.has("rooms") || !json.get("rooms").isJsonArray()) {
            throw new IllegalArgumentException("Theme is missing required array field 'rooms'");
        }

        JsonArray roomArray = json.getAsJsonArray("rooms");
        List<RoomStructure> rooms = new ArrayList<>(roomArray.size());
        for (JsonElement element : roomArray) {
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
                rooms.add(new RoomStructure(new StructureRef(ResourceLocation.parse(element.getAsString()), 0), 1));
                continue;
            }

            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("Room entries must be either a string id or an object");
            }

            JsonObject roomObject = element.getAsJsonObject();
            if (!roomObject.has("id")) {
                throw new IllegalArgumentException("Room object is missing required field 'id'");
            }

            StructureRef structureRef = readStructureRef(roomObject);
            int weight = roomObject.has("weight") ? roomObject.get("weight").getAsInt() : 1;
            rooms.add(new RoomStructure(structureRef, Math.max(1, weight)));
        }

        return List.copyOf(rooms);
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

    public record RoomStructure(StructureRef structure, int weight) {
    }
}
