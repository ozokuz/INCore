package ozokuz.incore.features.encounter_spawner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class EncounterManager extends SimpleJsonResourceReloadListener {
    public static final Map<ResourceLocation, EncounterData> ENCOUNTERS = new HashMap<>();

    public EncounterManager() {
        super(new Gson(), "encounters");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        ENCOUNTERS.clear();

        jsons.forEach((id, json) -> ENCOUNTERS.put(id, EncounterData.fromJson(json.getAsJsonObject())));
    }
}
