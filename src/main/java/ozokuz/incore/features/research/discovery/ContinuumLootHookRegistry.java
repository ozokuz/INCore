package ozokuz.incore.features.research.discovery;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ozokuz.incore.INCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContinuumLootHookRegistry extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ContinuumLootHookDefinition> definitions = Map.of();

    public ContinuumLootHookRegistry() {
        super(new Gson(), "research_continuum_loot_hooks");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ContinuumLootHookDefinition> next = new LinkedHashMap<>();
        jsons.forEach((fileId, element) -> {
            if (!element.isJsonObject()) {
                return;
            }
            ContinuumLootHookDefinition definition = parseDefinition(fileId, element.getAsJsonObject());
            if (definition != null) {
                next.put(definition.id(), definition);
            }
        });
        definitions = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} continuum loot hook placeholders.", definitions.size());
    }

    private static ContinuumLootHookDefinition parseDefinition(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = parseId(json, "id");
        if (id == null) {
            id = fileId;
        }
        List<ResourceLocation> lootTables = parseIds(json.getAsJsonArray("loot_tables"));
        List<WeightedReport> reports = new ArrayList<>();
        JsonArray reportArray = json.getAsJsonArray("reports");
        if (reportArray != null) {
            for (JsonElement element : reportArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                ResourceLocation reportId = parseId(row, "report_id");
                int weight = row.has("weight") ? Math.max(1, row.get("weight").getAsInt()) : 1;
                if (reportId != null) {
                    reports.add(new WeightedReport(reportId, weight));
                }
            }
        }
        return new ContinuumLootHookDefinition(id, List.copyOf(lootTables), List.copyOf(reports));
    }

    private static ResourceLocation parseId(JsonObject json, String key) {
        if (json == null || !json.has(key)) {
            return null;
        }
        return ResourceLocation.tryParse(json.get(key).getAsString());
    }

    private static List<ResourceLocation> parseIds(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<ResourceLocation> ids = new ArrayList<>();
        for (JsonElement element : array) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            if (id != null) {
                ids.add(id);
            }
        }
        return List.copyOf(ids);
    }

    public record ContinuumLootHookDefinition(
            ResourceLocation id,
            List<ResourceLocation> lootTables,
            List<WeightedReport> reports
    ) {
    }

    public record WeightedReport(ResourceLocation reportId, int weight) {
    }
}
