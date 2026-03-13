package io.github.ozokuz.incore.features.research.registry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.research.model.ResearchCategoryDefinition;
import io.github.ozokuz.incore.features.research.model.ResearchCostDefinition;
import io.github.ozokuz.incore.features.research.model.ResearchNodeDefinition;
import io.github.ozokuz.incore.features.research.model.ResearchPowerDefinition;
import io.github.ozokuz.incore.features.research.model.ResearchTreeDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ResearchRegistry extends SimplePreparableReloadListener<ResearchRegistry.LoadedResearchData> {
    private static final String LEGACY_ROOT = "research";
    private static final String CATEGORY_ROOT = "research_categories";
    private static final String TREE_ROOT = "research_trees";
    private static final String NODE_ROOT = "research_nodes";

    private static volatile Map<ResourceLocation, ResearchTreeDefinition> trees = Map.of();
    private static volatile Map<ResourceLocation, ResearchCategoryDefinition> categories = Map.of();
    private static volatile Map<ResourceLocation, ResearchNodeDefinition> nodes = Map.of();

    @Override
    protected @NotNull LoadedResearchData prepare(@NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        warnLegacyResources(resourceManager);
        Map<ResourceLocation, ResearchCategoryDefinition> parsedCategories = loadDefinitions(
                resourceManager,
                CATEGORY_ROOT,
                ResearchRegistry::parseCategoryFile
        );
        Map<ResourceLocation, ResearchTreeDefinition> parsedTrees = loadDefinitions(
                resourceManager,
                TREE_ROOT,
                ResearchRegistry::parseTreeFile
        );
        Map<ResourceLocation, ResearchNodeDefinition> parsedNodes = loadDefinitions(
                resourceManager,
                NODE_ROOT,
                ResearchRegistry::parseNodeFile
        );
        return new LoadedResearchData(parsedCategories, parsedTrees, parsedNodes);
    }

    @Override
    protected void apply(@NotNull LoadedResearchData prepared, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ResearchCategoryDefinition> nextCategories = new LinkedHashMap<>(prepared.categories());
        Map<ResourceLocation, ResearchTreeDefinition> nextTrees = new LinkedHashMap<>(prepared.trees());
        Map<ResourceLocation, ResearchNodeDefinition> nextNodes = new LinkedHashMap<>();

        prepared.nodes().forEach((id, node) -> {
            if (!nextTrees.containsKey(node.treeId())) {
                INCore.LOGGER.warn("Skipping research node '{}' because tree '{}' does not exist.", id, node.treeId());
                return;
            }
            if (!nextCategories.containsKey(node.categoryId())) {
                INCore.LOGGER.warn("Skipping research node '{}' because category '{}' does not exist.", id, node.categoryId());
                return;
            }

            List<ResourceLocation> filteredPrerequisites = new ArrayList<>();
            for (ResourceLocation prerequisite : node.prerequisites()) {
                if (prepared.nodes().containsKey(prerequisite)) {
                    filteredPrerequisites.add(prerequisite);
                } else {
                    INCore.LOGGER.warn("Dropping unknown prerequisite '{}' from research node '{}'.", prerequisite, id);
                }
            }

            nextNodes.put(id, new ResearchNodeDefinition(
                    node.id(),
                    node.name(),
                    node.treeId(),
                    node.categoryId(),
                    List.copyOf(filteredPrerequisites),
                    node.discoveryRules(),
                    node.researchCost(),
                    node.researchPower(),
                    node.researchTime(),
                    node.requiredRuns(),
                    node.outputs()
            ));
        });

        categories = Map.copyOf(nextCategories);
        trees = Map.copyOf(nextTrees);
        nodes = Map.copyOf(nextNodes);

        INCore.LOGGER.info("Loaded research registry: {} categories, {} trees, {} nodes.", categories.size(), trees.size(), nodes.size());
    }

    public static Map<ResourceLocation, ResearchTreeDefinition> trees() {
        return trees;
    }

    public static Map<ResourceLocation, ResearchCategoryDefinition> categories() {
        return categories;
    }

    public static Map<ResourceLocation, ResearchNodeDefinition> nodes() {
        return nodes;
    }

    private static <T> Map<ResourceLocation, T> loadDefinitions(
            ResourceManager resourceManager,
            String root,
            Function<ParsedFile, @Nullable T> parser
    ) {
        Map<ResourceLocation, T> output = new LinkedHashMap<>();
        resourceManager.listResources(root, location -> location.getPath().endsWith(".json")).entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> {
                    ResourceLocation resourcePath = entry.getKey();
                    ResourceLocation logicalId = toLogicalId(root, resourcePath);
                    if (logicalId == null) {
                        INCore.LOGGER.warn("Skipping research file '{}' because it does not map cleanly under '{}'.", resourcePath, root);
                        return;
                    }

                    JsonObject json = readJsonObject(resourcePath, entry.getValue());
                    if (json == null) {
                        return;
                    }

                    T parsed = parser.apply(new ParsedFile(resourcePath, logicalId, json));
                    if (parsed != null) {
                        output.put(logicalId, parsed);
                    }
                });
        return output;
    }

    private static @Nullable ResearchCategoryDefinition parseCategoryFile(ParsedFile file) {
        ResourceLocation icon = null;
        if (file.json().has("icon")) {
            icon = ResourceLocation.tryParse(file.json().get("icon").getAsString());
            if (icon == null) {
                INCore.LOGGER.warn("Skipping research category '{}' because icon '{}' is invalid.", file.resourcePath(), file.json().get("icon"));
                return null;
            }
        }

        return new ResearchCategoryDefinition(
                file.logicalId(),
                stringOr(file.json(), "name", humanizePath(file.logicalId().getPath())),
                icon
        );
    }

    private static @Nullable ResearchTreeDefinition parseTreeFile(ParsedFile file) {
        return new ResearchTreeDefinition(
                file.logicalId(),
                stringOr(file.json(), "name", humanizePath(file.logicalId().getPath())),
                stringOr(file.json(), "planet_theme", "")
        );
    }

    private static @Nullable ResearchNodeDefinition parseNodeFile(ParsedFile file) {
        ResourceLocation treeId = parseId(file.json(), "tree_id");
        ResourceLocation categoryId = parseId(file.json(), "category_id");
        if (treeId == null || categoryId == null) {
            INCore.LOGGER.warn("Skipping research node '{}' because tree_id or category_id is missing/invalid.", file.resourcePath());
            return null;
        }

        return new ResearchNodeDefinition(
                file.logicalId(),
                stringOr(file.json(), "name", humanizePath(file.logicalId().getPath())),
                treeId,
                categoryId,
                List.copyOf(parseIdList(file.json().getAsJsonArray("prerequisites"))),
                nullableString(file.json(), "discovery_rules"),
                parseCost(file.json().getAsJsonObject("research_cost")),
                parsePower(file.json().getAsJsonObject("research_power")),
                intOr(file.json(), "research_time", 200, 1),
                intOr(file.json(), "required_runs", 3, 1),
                List.copyOf(parseStringList(file.json().getAsJsonArray("outputs")))
        );
    }

    private static void warnLegacyResources(ResourceManager resourceManager) {
        resourceManager.listResources(LEGACY_ROOT, location -> location.getPath().endsWith(".json")).keySet().stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .forEach(location -> INCore.LOGGER.warn(
                        "Ignoring legacy research datapack '{}' . Migrate content into '{}', '{}', and '{}'.",
                        location,
                        CATEGORY_ROOT,
                        TREE_ROOT,
                        NODE_ROOT
                ));
    }

    private static @Nullable JsonObject readJsonObject(ResourceLocation resourcePath, net.minecraft.server.packs.resources.Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            JsonElement element = GsonHelper.parse(reader);
            if (!element.isJsonObject()) {
                INCore.LOGGER.warn("Skipping research file '{}' because the root JSON is not an object.", resourcePath);
                return null;
            }
            return element.getAsJsonObject();
        } catch (Exception e) {
            INCore.LOGGER.error("Failed to parse research file '{}'.", resourcePath, e);
            return null;
        }
    }

    private static @Nullable ResourceLocation toLogicalId(String root, ResourceLocation resourcePath) {
        String path = resourcePath.getPath();
        String prefix = root + "/";
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            return null;
        }
        String trimmed = path.substring(prefix.length(), path.length() - ".json".length());
        if (trimmed.isBlank()) {
            return null;
        }
        return ResourceLocation.fromNamespaceAndPath(resourcePath.getNamespace(), trimmed);
    }

    private static ResearchCostDefinition parseCost(JsonObject costObject) {
        if (costObject == null) {
            return new ResearchCostDefinition(List.of(), List.of(), List.of());
        }

        List<ResearchCostDefinition.LogicModuleRequirement> logicModules = new ArrayList<>();
        JsonArray logicArray = costObject.getAsJsonArray("required_logic_modules");
        if (logicArray != null) {
            for (JsonElement element : logicArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                String tier = stringOr(row, "module_tier", "");
                int durabilityCost = row.has("durability_cost") ? Math.max(0, row.get("durability_cost").getAsInt()) : 0;
                if (!tier.isBlank() && durabilityCost > 0) {
                    logicModules.add(new ResearchCostDefinition.LogicModuleRequirement(tier, durabilityCost));
                }
            }
        }

        List<ResearchCostDefinition.ResearchMaterialRequirement> materials = new ArrayList<>();
        JsonArray materialArray = costObject.getAsJsonArray("required_research_materials");
        if (materialArray != null) {
            for (JsonElement element : materialArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                String materialId = stringOr(row, "material_id", "");
                int count = row.has("count") ? Math.max(0, row.get("count").getAsInt()) : 0;
                if (!materialId.isBlank() && count > 0) {
                    materials.add(new ResearchCostDefinition.ResearchMaterialRequirement(materialId, count));
                }
            }
        }

        List<ResearchCostDefinition.CostModifier> modifiers = new ArrayList<>();
        JsonArray modifierArray = costObject.getAsJsonArray("modifiers");
        if (modifierArray != null) {
            for (JsonElement element : modifierArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject row = element.getAsJsonObject();
                String modifierId = stringOr(row, "id", "");
                double value = row.has("value") ? row.get("value").getAsDouble() : 0.0D;
                if (!modifierId.isBlank()) {
                    modifiers.add(new ResearchCostDefinition.CostModifier(modifierId, value));
                }
            }
        }

        return new ResearchCostDefinition(List.copyOf(logicModules), List.copyOf(materials), List.copyOf(modifiers));
    }

    private static ResearchPowerDefinition parsePower(JsonObject powerObject) {
        if (powerObject == null) {
            return ResearchPowerDefinition.defaults();
        }

        double base = powerObject.has("base_rp_per_tick") ? Math.max(0.0D, powerObject.get("base_rp_per_tick").getAsDouble()) : 1.0D;
        double scale = powerObject.has("curve_scale_rp_per_tick") ? Math.max(0.0D, powerObject.get("curve_scale_rp_per_tick").getAsDouble()) : 0.0D;
        double exponent = powerObject.has("curve_exponent") ? Math.max(0.0D, powerObject.get("curve_exponent").getAsDouble()) : 1.0D;
        return new ResearchPowerDefinition(base, scale, exponent);
    }

    private static List<ResourceLocation> parseIdList(JsonArray array) {
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
        return ids;
    }

    private static List<String> parseStringList(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement element : array) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static @Nullable ResourceLocation parseId(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return null;
        }
        return ResourceLocation.tryParse(object.get(key).getAsString());
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        return object.get(key).getAsString();
    }

    private static @Nullable String nullableString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return null;
        }
        String value = object.get(key).getAsString();
        return value.isBlank() ? null : value;
    }

    private static int intOr(JsonObject object, String key, int fallback, int minimum) {
        if (object == null || !object.has(key)) {
            return fallback;
        }
        return Math.max(minimum, object.get(key).getAsInt());
    }

    private static String humanizePath(String path) {
        if (path == null || path.isBlank()) {
            return "Unknown";
        }

        String[] parts = path.split("[/_-]");
        StringBuilder builder = new StringBuilder(path.length() + 8);
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            char first = part.charAt(0);
            builder.append(Character.toUpperCase(first));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }

        return builder.length() == 0 ? "Unknown" : builder.toString();
    }

    private record ParsedFile(ResourceLocation resourcePath, ResourceLocation logicalId, JsonObject json) {
    }

    protected record LoadedResearchData(
            Map<ResourceLocation, ResearchCategoryDefinition> categories,
            Map<ResourceLocation, ResearchTreeDefinition> trees,
            Map<ResourceLocation, ResearchNodeDefinition> nodes
    ) {
    }
}
