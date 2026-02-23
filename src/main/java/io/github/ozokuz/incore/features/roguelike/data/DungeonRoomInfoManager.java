package io.github.ozokuz.incore.features.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import io.github.ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class DungeonRoomInfoManager extends SimpleJsonResourceReloadListener {
    public static final Map<ResourceLocation, DungeonRoomInfoData> ROOM_INFOS = new LinkedHashMap<>();
    public static final Map<ResourceLocation, DungeonRoomInfoData> ROOM_INFOS_BY_STRUCTURE = new LinkedHashMap<>();

    public DungeonRoomInfoManager() {
        super(new Gson(), "roguelike/rooms");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        ROOM_INFOS.clear();
        ROOM_INFOS_BY_STRUCTURE.clear();

        jsons.forEach((id, json) -> {
            try {
                DungeonRoomInfoData roomInfo = DungeonRoomInfoData.fromJson(json.getAsJsonObject());
                ROOM_INFOS.put(id, roomInfo);

                DungeonRoomInfoData previous = ROOM_INFOS_BY_STRUCTURE.putIfAbsent(roomInfo.structureId(), roomInfo);
                if (previous != null) {
                    INCore.LOGGER.warn("Duplicate roguelike room info structure id {} in {}; keeping first loaded entry", roomInfo.structureId(), id);
                }
            } catch (Exception e) {
                INCore.LOGGER.warn("Failed to load roguelike room info {}", id, e);
            }
        });

        INCore.LOGGER.info("Loaded {} roguelike room infos ({} unique structures)", ROOM_INFOS.size(), ROOM_INFOS_BY_STRUCTURE.size());
    }

    public static Optional<DungeonRoomInfoData> byStructure(ResourceLocation structureId) {
        return Optional.ofNullable(ROOM_INFOS_BY_STRUCTURE.get(structureId));
    }
}
