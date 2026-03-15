package ozokuz.incore.features.research.discovery;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import ozokuz.incore.INCore;
import ozokuz.incore.features.research.registry.ResearchRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EnvironmentReportRegistry extends SimpleJsonResourceReloadListener {
    private static volatile List<EnvironmentReportDefinition> definitions = List.of();

    public EnvironmentReportRegistry() {
        super(new Gson(), "research_environment_reports");
    }

    @Override
    protected void apply(java.util.Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        List<EnvironmentReportDefinition> next = new ArrayList<>();
        jsons.forEach((fileId, element) -> {
            if (!element.isJsonObject()) {
                return;
            }
            EnvironmentReportDefinition definition = parseDefinition(fileId, element.getAsJsonObject());
            if (definition != null) {
                next.add(definition);
            }
        });
        next.sort(Comparator.comparing(definition -> definition.id().toString()));
        definitions = List.copyOf(next);
        INCore.LOGGER.info("Loaded {} environment report definitions.", definitions.size());
    }

    public static List<EnvironmentReportDefinition> matching(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return List.of();
        }
        List<EnvironmentReportDefinition> matches = new ArrayList<>();
        for (EnvironmentReportDefinition definition : definitions) {
            if (definition.matches(level, pos)) {
                matches.add(definition);
            }
        }
        return List.copyOf(matches);
    }

    private static EnvironmentReportDefinition parseDefinition(ResourceLocation fileId, JsonObject json) {
        ResourceLocation id = parseId(json, "id");
        if (id == null) {
            id = fileId;
        }
        List<ResourceLocation> nodeIds = parseNodeIds(json.getAsJsonArray("node_ids"));
        if (nodeIds.isEmpty()) {
            return null;
        }
        Set<ResourceLocation> biomeIds = new LinkedHashSet<>(parseIds(json.getAsJsonArray("biomes")));
        ResourceLocation dimensionId = parseId(json, "dimension");
        Integer minY = json.has("min_y") ? json.get("min_y").getAsInt() : null;
        Integer maxY = json.has("max_y") ? json.get("max_y").getAsInt() : null;
        String displayName = json.has("display_name") ? json.get("display_name").getAsString() : humanize(id);
        return new EnvironmentReportDefinition(id, displayName, dimensionId, Set.copyOf(biomeIds), minY, maxY, List.copyOf(nodeIds));
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
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    private static List<ResourceLocation> parseNodeIds(JsonArray array) {
        if (array == null) {
            return List.of();
        }
        List<ResourceLocation> ids = new ArrayList<>();
        for (JsonElement element : array) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            if (id != null && ResearchRegistry.nodes().containsKey(id)) {
                ids.add(id);
            }
        }
        ids.sort(Comparator.naturalOrder());
        return List.copyOf(ids);
    }

    private static String humanize(ResourceLocation id) {
        String[] parts = id.getPath().split("[_/-]");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? id.toString() : builder.toString();
    }

    public record EnvironmentReportDefinition(
            ResourceLocation id,
            String displayName,
            ResourceLocation dimensionId,
            Set<ResourceLocation> biomeIds,
            Integer minY,
            Integer maxY,
            List<ResourceLocation> nodeIds
    ) {
        public boolean matches(Level level, BlockPos pos) {
            if (dimensionId != null && !dimensionId.equals(level.dimension().location())) {
                return false;
            }
            if (minY != null && pos.getY() < minY) {
                return false;
            }
            if (maxY != null && pos.getY() > maxY) {
                return false;
            }
            if (biomeIds.isEmpty()) {
                return true;
            }
            ResourceLocation biomeId = level.getBiome(pos).unwrapKey().map(ResourceKey::location).orElse(null);
            return biomeId != null && biomeIds.contains(biomeId);
        }
    }
}
