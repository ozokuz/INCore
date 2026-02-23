package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public record DungeonThemeData(
        TemplateRef startRoom,
        Direction startExit,
        HallwayPools hallways,
        Map<RoomType, TemplatePool> roomPools,
        Map<RoomType, Integer> roomTypeWeights,
        SpecialRoomQuotas specialRoomQuotas,
        TemplatePool secretRooms,
        int weight
) {
    private static final String KEY_START_ROOM_NEW = "start_room";
    private static final String KEY_START_ROOM_LEGACY = "starting_room";

    public DungeonThemeData {
        startRoom = startRoom == null ? new TemplateRef(ResourceLocation.parse("minecraft:empty"), 0, 1) : startRoom;
        startExit = startExit == null ? Direction.SOUTH : startExit;
        hallways = hallways == null ? new HallwayPools(TemplatePool.EMPTY, TemplatePool.EMPTY) : hallways;
        roomPools = roomPools == null ? Map.of() : Map.copyOf(roomPools);
        roomTypeWeights = roomTypeWeights == null ? Map.of() : Map.copyOf(roomTypeWeights);
        specialRoomQuotas = specialRoomQuotas == null ? new SpecialRoomQuotas(0, 0, 0) : specialRoomQuotas;
        secretRooms = secretRooms == null ? TemplatePool.EMPTY : secretRooms;
        weight = Math.max(1, weight);
    }

    public static DungeonThemeData fromJson(JsonObject json) {
        TemplateRef startRoom = requiredTemplateRef(json, KEY_START_ROOM_NEW, KEY_START_ROOM_LEGACY);
        Direction startExit = readDirection(json, "start_exit", Direction.SOUTH);
        HallwayPools hallways = readHallwayPools(json);
        Map<RoomType, TemplatePool> roomPools = readRoomPools(json);
        Map<RoomType, Integer> roomTypeWeights = readRoomTypeWeights(json, roomPools);
        SpecialRoomQuotas specialRoomQuotas = readSpecialRoomQuotas(json, roomPools);
        TemplatePool secretRooms = readSecretRooms(json, startRoom);
        int weight = json.has("weight") ? json.get("weight").getAsInt() : 1;
        return new DungeonThemeData(
                startRoom,
                startExit,
                hallways,
                roomPools,
                roomTypeWeights,
                specialRoomQuotas,
                secretRooms,
                Math.max(1, weight)
        );
    }

    @Deprecated(forRemoval = false)
    public TemplateRef startingRoomStructure() {
        return startRoom;
    }

    public TemplatePool roomPool(RoomType type) {
        return roomPools.getOrDefault(type, TemplatePool.EMPTY);
    }

    public int roomTypeWeight(RoomType type) {
        return roomTypeWeights.getOrDefault(type, 1);
    }

    public Set<ResourceLocation> allTemplateIds() {
        Set<ResourceLocation> ids = new HashSet<>();
        ids.add(startRoom.id());
        ids.addAll(hallways.allTemplateIds());
        for (TemplatePool pool : roomPools.values()) {
            ids.addAll(pool.templateIds());
        }
        ids.addAll(secretRooms.templateIds());
        return ids;
    }

    private static TemplateRef requiredTemplateRef(JsonObject json, String... keys) {
        for (String key : keys) {
            if (json.has(key)) {
                return readTemplateRef(json.get(key), false);
            }
        }
        throw new IllegalArgumentException("Theme is missing required field '" + keys[0] + "'");
    }

    private static HallwayPools readHallwayPools(JsonObject json) {
        if (json.has("hallways")) {
            JsonObject hallwaysObject = json.getAsJsonObject("hallways");
            TemplatePool northSouth = readTemplatePool(hallwaysObject, "north_south", false);
            if (northSouth.isEmpty()) {
                northSouth = readTemplatePool(hallwaysObject, "ns", false);
            }

            TemplatePool eastWest = readTemplatePool(hallwaysObject, "east_west", false);
            if (eastWest.isEmpty()) {
                eastWest = readTemplatePool(hallwaysObject, "ew", false);
            }

            return new HallwayPools(northSouth, eastWest);
        }

        TemplatePool northSouth = readTemplatePool(json, "hallway_ns", false);
        TemplatePool eastWest = readTemplatePool(json, "hallway_ew", false);
        return new HallwayPools(northSouth, eastWest);
    }

    private static Map<RoomType, TemplatePool> readRoomPools(JsonObject json) {
        EnumMap<RoomType, TemplatePool> pools = new EnumMap<>(RoomType.class);
        for (RoomType type : RoomType.values()) {
            pools.put(type, TemplatePool.EMPTY);
        }

        if (json.has("room_types")) {
            JsonObject roomTypes = json.getAsJsonObject("room_types");
            for (RoomType type : RoomType.values()) {
                pools.put(type, readTemplatePool(roomTypes, type.jsonKey(), true));
            }
        }

        if (json.has("rooms")) {
            TemplatePool legacyPool = readTemplatePool(json, "rooms", true);
            TemplatePool existingRegular = pools.getOrDefault(RoomType.REGULAR, TemplatePool.EMPTY);
            pools.put(RoomType.REGULAR, existingRegular.isEmpty() ? legacyPool : existingRegular);
        }

        return Map.copyOf(pools);
    }

    private static Map<RoomType, Integer> readRoomTypeWeights(JsonObject json, Map<RoomType, TemplatePool> pools) {
        EnumMap<RoomType, Integer> weights = new EnumMap<>(RoomType.class);
        JsonObject rawWeights = json.has("room_type_weights") ? json.getAsJsonObject("room_type_weights") : null;
        for (RoomType type : RoomType.values()) {
            if (pools.getOrDefault(type, TemplatePool.EMPTY).isEmpty()) {
                continue;
            }

            int value = 1;
            if (rawWeights != null && rawWeights.has(type.jsonKey())) {
                value = Math.max(1, rawWeights.get(type.jsonKey()).getAsInt());
            }
            weights.put(type, value);
        }
        return Map.copyOf(weights);
    }

    private static SpecialRoomQuotas readSpecialRoomQuotas(JsonObject json, Map<RoomType, TemplatePool> pools) {
        JsonObject quotasObject = json.has("special_room_quotas") ? json.getAsJsonObject("special_room_quotas") : new JsonObject();
        int library = readSpecialQuota(quotasObject, RoomType.LIBRARY, pools);
        int factory = readSpecialQuota(quotasObject, RoomType.FACTORY, pools);
        int workshop = readSpecialQuota(quotasObject, RoomType.WORKSHOP, pools);
        return new SpecialRoomQuotas(library, factory, workshop);
    }

    private static int readSpecialQuota(JsonObject quotasObject, RoomType type, Map<RoomType, TemplatePool> pools) {
        if (pools.getOrDefault(type, TemplatePool.EMPTY).isEmpty()) {
            return 0;
        }
        int value = quotasObject.has(type.jsonKey()) ? quotasObject.get(type.jsonKey()).getAsInt() : 1;
        return Math.max(0, value);
    }

    private static TemplatePool readSecretRooms(JsonObject json, TemplateRef startRoom) {
        if (json.has("secret_rooms")) {
            TemplatePool pool = readTemplatePool(json, "secret_rooms", true);
            if (!pool.isEmpty()) {
                return pool;
            }
        }

        return new TemplatePool(List.of(new TemplateRef(startRoom.id(), startRoom.middleFloorY(), 1)));
    }

    private static Direction readDirection(JsonObject json, String key, Direction fallback) {
        if (!json.has(key)) {
            return fallback;
        }

        String value = json.get(key).getAsString().trim().toLowerCase();
        return switch (value) {
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "east" -> Direction.EAST;
            case "west" -> Direction.WEST;
            default -> fallback;
        };
    }

    private static TemplatePool readTemplatePool(JsonObject json, String key, boolean requireMiddleFloorY) {
        if (!json.has(key)) {
            return TemplatePool.EMPTY;
        }
        return readTemplatePool(json.get(key), requireMiddleFloorY);
    }

    private static TemplatePool readTemplatePool(JsonElement element, boolean requireMiddleFloorY) {
        if (element == null || element.isJsonNull()) {
            return TemplatePool.EMPTY;
        }

        if (element.isJsonArray()) {
            List<TemplateRef> refs = new ArrayList<>();
            for (JsonElement row : element.getAsJsonArray()) {
                refs.add(readTemplateRef(row, requireMiddleFloorY));
            }
            return new TemplatePool(refs);
        }

        return new TemplatePool(List.of(readTemplateRef(element, requireMiddleFloorY)));
    }

    private static TemplateRef readTemplateRef(JsonElement element, boolean requireMiddleFloorY) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            if (requireMiddleFloorY) {
                throw new IllegalArgumentException("Room template references must define 'middle_floor_y'");
            }
            return new TemplateRef(ResourceLocation.parse(element.getAsString()), 0, 1);
        }

        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Template reference must be a string id or an object");
        }

        JsonObject object = element.getAsJsonObject();
        if (!object.has("id")) {
            throw new IllegalArgumentException("Template reference object is missing required field 'id'");
        }

        ResourceLocation id = ResourceLocation.parse(object.get("id").getAsString());
        int middleFloorY;
        if (object.has("middle_floor_y")) {
            middleFloorY = Math.max(0, object.get("middle_floor_y").getAsInt());
        } else if (object.has("floor_y_from_bottom")) {
            middleFloorY = Math.max(0, object.get("floor_y_from_bottom").getAsInt());
        } else if (requireMiddleFloorY) {
            throw new IllegalArgumentException("Template reference for '" + id + "' is missing required field 'middle_floor_y'");
        } else {
            middleFloorY = 0;
        }

        int weight = object.has("weight") ? object.get("weight").getAsInt() : 1;
        return new TemplateRef(id, middleFloorY, Math.max(1, weight));
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

        private final String jsonKey;

        RoomType(String jsonKey) {
            this.jsonKey = jsonKey;
        }

        public String jsonKey() {
            return jsonKey;
        }

        public boolean isSpecial() {
            return this == LIBRARY || this == FACTORY || this == WORKSHOP;
        }
    }

    public record TemplateRef(ResourceLocation id, int middleFloorY, int weight) {
        public int originYForMiddleFloor(int worldMiddleFloorY) {
            return worldMiddleFloorY - Math.max(0, middleFloorY);
        }
    }

    public record TemplatePool(List<TemplateRef> templates) {
        public static final TemplatePool EMPTY = new TemplatePool(List.of());

        public TemplatePool {
            templates = templates == null ? List.of() : List.copyOf(templates);
        }

        public boolean isEmpty() {
            return templates.isEmpty();
        }

        public Optional<TemplateRef> pick(RandomSource random) {
            if (templates.isEmpty()) {
                return Optional.empty();
            }

            int totalWeight = 0;
            for (TemplateRef template : templates) {
                totalWeight += Math.max(1, template.weight());
            }
            if (totalWeight <= 0) {
                return Optional.of(templates.get(0));
            }

            int roll = random.nextInt(totalWeight);
            for (TemplateRef template : templates) {
                roll -= Math.max(1, template.weight());
                if (roll < 0) {
                    return Optional.of(template);
                }
            }

            return Optional.of(templates.get(0));
        }

        public Set<ResourceLocation> templateIds() {
            Set<ResourceLocation> ids = new HashSet<>();
            for (TemplateRef template : templates) {
                ids.add(template.id());
            }
            return ids;
        }
    }

    public record HallwayPools(TemplatePool northSouth, TemplatePool eastWest) {
        public HallwayPools {
            northSouth = northSouth == null ? TemplatePool.EMPTY : northSouth;
            eastWest = eastWest == null ? TemplatePool.EMPTY : eastWest;
        }

        public Set<ResourceLocation> allTemplateIds() {
            Set<ResourceLocation> ids = new HashSet<>();
            ids.addAll(northSouth.templateIds());
            ids.addAll(eastWest.templateIds());
            return ids;
        }
    }

    public record SpecialRoomQuotas(int library, int factory, int workshop) {
        public SpecialRoomQuotas {
            library = Math.max(0, library);
            factory = Math.max(0, factory);
            workshop = Math.max(0, workshop);
        }
    }
}
