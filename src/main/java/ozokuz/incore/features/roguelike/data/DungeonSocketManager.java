package ozokuz.incore.features.roguelike.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public class DungeonSocketManager extends SimpleJsonResourceReloadListener {
    public static final Map<ResourceLocation, DungeonSocketData> SOCKETS = new LinkedHashMap<>();

    public DungeonSocketManager() {
        super(new Gson(), "roguelike/sockets");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        SOCKETS.clear();

        jsons.forEach((id, json) -> {
            try {
                SOCKETS.put(id, DungeonSocketData.fromJson(json.getAsJsonObject()));
            } catch (Exception e) {
                INCore.LOGGER.warn("Failed to load roguelike socket metadata {}", id, e);
            }
        });

        INCore.LOGGER.info("Loaded {} roguelike socket metadata entries", SOCKETS.size());
    }
}
