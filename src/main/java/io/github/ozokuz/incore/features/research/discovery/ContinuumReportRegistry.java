package io.github.ozokuz.incore.features.research.discovery;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.registry.ResearchRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContinuumReportRegistry extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ContinuumReportDefinition> definitions = Map.of();

    public ContinuumReportRegistry() {
        super(new Gson(), "research_continuum_reports");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ContinuumReportDefinition> next = new LinkedHashMap<>();
        jsons.forEach((fileId, element) -> {
            if (!element.isJsonObject()) {
                return;
            }
            ContinuumReportDefinition definition = parseDefinition(fileId, element.getAsJsonObject());
            if (definition != null) {
                next.put(definition.id(), definition);
            }
        });
        definitions = Map.copyOf(next);
        INCore.LOGGER.info("Loaded {} continuum report definitions.", definitions.size());
    }

    public static ContinuumReportDefinition get(ResourceLocation id) {
        return id == null ? null : definitions.get(id);
    }

    private static ContinuumReportDefinition parseDefinition(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = parseId(json, "id");
        if (id == null) {
            id = fileId;
        }
        List<ResourceLocation> nodeIds = parseNodeIds(json.getAsJsonArray("node_ids"));
        if (nodeIds.isEmpty()) {
            return null;
        }
        String displayName = json.has("display_name") ? json.get("display_name").getAsString() : id.toString();
        return new ContinuumReportDefinition(id, displayName, List.copyOf(nodeIds));
    }

    private static ResourceLocation parseId(JsonObject json, String key) {
        if (json == null || !json.has(key)) {
            return null;
        }
        return ResourceLocation.tryParse(json.get(key).getAsString());
    }

    private static List<ResourceLocation> parseNodeIds(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<ResourceLocation> nodeIds = new ArrayList<>();
        for (JsonElement element : array) {
            ResourceLocation nodeId = ResourceLocation.tryParse(element.getAsString());
            if (nodeId != null && ResearchRegistry.nodes().containsKey(nodeId)) {
                nodeIds.add(nodeId);
            }
        }
        nodeIds.sort(Comparator.naturalOrder());
        return List.copyOf(nodeIds);
    }

    public record ContinuumReportDefinition(
            ResourceLocation id,
            String displayName,
            List<ResourceLocation> nodeIds
    ) {
    }
}
