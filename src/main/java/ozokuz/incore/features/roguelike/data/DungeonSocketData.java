package ozokuz.incore.features.roguelike.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record DungeonSocketData(
        List<EntrySocket> entrySockets,
        List<SecretSocket> secretSockets,
        List<FeatureSocket> featureSockets
) {
    public static final DungeonSocketData EMPTY = new DungeonSocketData(List.of(), List.of(), List.of());

    public DungeonSocketData {
        entrySockets = entrySockets == null ? List.of() : List.copyOf(entrySockets);
        secretSockets = secretSockets == null ? List.of() : List.copyOf(secretSockets);
        featureSockets = featureSockets == null ? List.of() : List.copyOf(featureSockets);
    }

    public static DungeonSocketData fromJson(JsonObject json) {
        return new DungeonSocketData(
                readEntrySockets(json),
                readSecretSockets(json),
                readFeatureSockets(json)
        );
    }

    private static List<EntrySocket> readEntrySockets(JsonObject json) {
        if (!json.has("entry_sockets") || !json.get("entry_sockets").isJsonArray()) {
            return List.of();
        }

        List<EntrySocket> sockets = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("entry_sockets")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            String id = object.has("id") ? object.get("id").getAsString() : "entry";
            sockets.add(new EntrySocket(id, readPos(object)));
        }
        return List.copyOf(sockets);
    }

    private static List<SecretSocket> readSecretSockets(JsonObject json) {
        if (!json.has("secret_sockets") || !json.get("secret_sockets").isJsonArray()) {
            return List.of();
        }

        List<SecretSocket> sockets = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("secret_sockets")) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            String id = object.has("id") ? object.get("id").getAsString() : "secret";
            int[] offset = readIntArray(object, "chunk_offset", 2);
            sockets.add(new SecretSocket(id, offset[0], offset[1]));
        }
        return List.copyOf(sockets);
    }

    private static List<FeatureSocket> readFeatureSockets(JsonObject json) {
        if (!json.has("feature_sockets") || !json.get("feature_sockets").isJsonArray()) {
            return List.of();
        }

        List<FeatureSocket> sockets = new ArrayList<>();
        for (JsonElement element : json.getAsJsonArray("feature_sockets")) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            String type = object.has("type") ? object.get("type").getAsString() : "";
            ResourceLocation encounterId = object.has("encounter")
                    ? ResourceLocation.tryParse(object.get("encounter").getAsString())
                    : null;
            ResourceLocation markerId = object.has("marker_id")
                    ? ResourceLocation.tryParse(object.get("marker_id").getAsString())
                    : null;
            int[] spawnOffsetRaw = readIntArray(object, "spawn_offset", 3);
            Vec3i spawnOffset = new Vec3i(spawnOffsetRaw[0], spawnOffsetRaw[1], spawnOffsetRaw[2]);
            sockets.add(new FeatureSocket(type, readPos(object), encounterId, spawnOffset, markerId, readBlockEntityData(object)));
        }
        return List.copyOf(sockets);
    }

    private static CompoundTag readBlockEntityData(JsonObject object) {
        if (!object.has("block_entity_nbt")) {
            return null;
        }
        try {
            return TagParser.parseTag(object.get("block_entity_nbt").getAsString());
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid block_entity_nbt string", exception);
        }
    }

    private static BlockPos readPos(JsonObject object) {
        if (object.has("pos")) {
            int[] values = readIntArray(object, "pos", 3);
            return new BlockPos(values[0], values[1], values[2]);
        }

        int x = object.has("x") ? object.get("x").getAsInt() : 0;
        int y = object.has("y") ? object.get("y").getAsInt() : 0;
        int z = object.has("z") ? object.get("z").getAsInt() : 0;
        return new BlockPos(x, y, z);
    }

    private static int[] readIntArray(JsonObject object, String key, int expectedLength) {
        int[] values = new int[expectedLength];
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            return values;
        }

        JsonArray array = object.getAsJsonArray(key);
        for (int i = 0; i < expectedLength && i < array.size(); i++) {
            values[i] = array.get(i).getAsInt();
        }
        return values;
    }

    public record EntrySocket(String id, BlockPos pos) {
    }

    public record SecretSocket(String id, int chunkOffsetX, int chunkOffsetZ) {
    }

    public record FeatureSocket(
            String type,
            BlockPos pos,
            ResourceLocation encounterId,
            Vec3i spawnOffset,
            ResourceLocation markerId,
            CompoundTag blockEntityData
    ) {
        public FeatureSocket {
            type = type == null ? "" : type;
            pos = pos == null ? BlockPos.ZERO : pos.immutable();
            spawnOffset = spawnOffset == null ? Vec3i.ZERO : spawnOffset;
            blockEntityData = blockEntityData == null ? null : blockEntityData.copy();
        }
    }
}
