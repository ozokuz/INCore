package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record DungeonRoomInfoData(
        ResourceLocation structureId,
        int connectionHeight,
        RoomType roomType,
        List<String> secrets,
        List<BlockPos> spawnerLocations,
        List<BlockPos> featureLocations,
        List<BlockPos> objectiveTaskLocations
) {
    public static DungeonRoomInfoData fromJson(JsonObject json) {
        ResourceLocation structureId = readStructureId(json);
        int connectionHeight = Math.clamp(readRequiredInt(json, "connection_height"), 0, 255);
        RoomType roomType = readRoomType(json);
        List<String> secrets = readSecrets(json);
        List<BlockPos> spawners = readPositions(json, "spawner_locations");
        List<BlockPos> features = readPositions(json, "feature_locations");
        List<BlockPos> objectiveTasks = readPositions(json, "objective_task_locations");

        return new DungeonRoomInfoData(
                structureId,
                connectionHeight,
                roomType,
                secrets,
                spawners,
                features,
                objectiveTasks
        );
    }

    private static ResourceLocation readStructureId(JsonObject json) {
        if (!json.has("structure_id")) {
            throw new IllegalArgumentException("Room info is missing required field 'structure_id'");
        }
        return ResourceLocation.parse(json.get("structure_id").getAsString());
    }

    private static int readRequiredInt(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalArgumentException("Room info is missing required field '" + key + "'");
        }
        return json.get(key).getAsInt();
    }

    private static RoomType readRoomType(JsonObject json) {
        if (!json.has("room_type")) {
            throw new IllegalArgumentException("Room info is missing required field 'room_type'");
        }

        String raw = json.get("room_type").getAsString();
        RoomType type = RoomType.fromSerializedName(raw);
        if (type == null) {
            throw new IllegalArgumentException("Unknown room_type '" + raw + "'");
        }

        return type;
    }

    private static List<String> readSecrets(JsonObject json) {
        if (!json.has("secrets")) {
            return List.of();
        }

        JsonElement element = json.get("secrets");
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("'secrets' must be an array");
        }

        JsonArray array = element.getAsJsonArray();
        List<String> result = new ArrayList<>(array.size());
        for (JsonElement secretElement : array) {
            if (!secretElement.isJsonPrimitive() || !secretElement.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Each entry in 'secrets' must be a string");
            }

            String value = secretElement.getAsString().trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }

        return List.copyOf(result);
    }

    private static List<BlockPos> readPositions(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }

        JsonElement element = json.get(key);
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("'" + key + "' must be an array of [x, y, z] entries");
        }

        JsonArray array = element.getAsJsonArray();
        List<BlockPos> positions = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            JsonElement entry = array.get(index);
            if (!entry.isJsonArray()) {
                throw new IllegalArgumentException("'" + key + "' entry " + index + " must be [x, y, z]");
            }

            JsonArray coords = entry.getAsJsonArray();
            if (coords.size() != 3) {
                throw new IllegalArgumentException("'" + key + "' entry " + index + " must contain exactly 3 values");
            }

            positions.add(new BlockPos(
                    coords.get(0).getAsInt(),
                    coords.get(1).getAsInt(),
                    coords.get(2).getAsInt()
            ));
        }

        return List.copyOf(positions);
    }

    public enum RoomType {
        REGULAR("regular"),
        QUARRY("quarry"),
        MARKET("market"),
        LOOT("loot"),
        NORTHIUM_CAVES("northium_caves"),
        LIBRARY("library"),
        FACTORY("factory"),
        WORKSHOP("workshop");

        private final String serializedName;

        RoomType(String serializedName) {
            this.serializedName = serializedName;
        }

        public String serializedName() {
            return serializedName;
        }

        public static RoomType fromSerializedName(String raw) {
            if (raw == null) {
                return null;
            }

            String normalized = raw.toLowerCase(Locale.ROOT);
            for (RoomType value : values()) {
                if (value.serializedName.equals(normalized)) {
                    return value;
                }
            }

            return null;
        }
    }
}
