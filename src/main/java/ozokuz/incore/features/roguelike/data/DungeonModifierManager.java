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

public class DungeonModifierManager extends SimpleJsonResourceReloadListener {
    public static final Map<ResourceLocation, DungeonModifierData> MODIFIERS = new LinkedHashMap<>();

    public DungeonModifierManager() {
        super(new Gson(), "roguelike/modifiers");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profiler) {
        MODIFIERS.clear();

        jsons.forEach((id, json) -> {
            try {
                MODIFIERS.put(id, DungeonModifierData.fromJson(json.getAsJsonObject()));
            } catch (Exception exception) {
                INCore.LOGGER.warn("Failed to load roguelike modifier {}", id, exception);
            }
        });

        INCore.LOGGER.info("Loaded {} roguelike dungeon modifiers", MODIFIERS.size());
    }
}
