package io.github.ozokuz.incore.features.researchv2.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.INCore;
import io.github.ozokuz.incore.features.researchv2.model.ResearchCategoryDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNetworkDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNodeDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchPowerDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchTreeDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ResearchRegistry extends SimpleJsonResourceReloadListener {
    private static volatile Map<ResourceLocation, ResearchTreeDefinition> trees = Map.of();
    private static volatile Map<ResourceLocation, ResearchCategoryDefinition> categories = Map.of();
    private static volatile Map<ResourceLocation, ResearchNodeDefinition> nodes = Map.of();
    private static volatile Map<ResourceLocation, ResearchNetworkDefinition> networks = Map.of();

    public ResearchRegistry() {
        super(new Gson(), "research_v2");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsons, @NotNull ResourceManager resourceManager, @NotNull ProfilerFiller profilerFiller) {
        Map<ResourceLocation, ResearchTreeDefinition> nextTrees = new LinkedHashMap<>();
        Map<ResourceLocation, ResearchCategoryDefinition> nextCategories = new LinkedHashMap<>();
        Map<ResourceLocation, ResearchNodeDefinition> nextNodes = new LinkedHashMap<>();
        Map<ResourceLocation, ResearchNetworkDefinition> nextNetworks = new LinkedHashMap<>();

        jsons.forEach((id, jsonElement) -> {
            if (!jsonElement.isJsonObject()) {
                return;
            }

            JsonObject root = jsonElement.getAsJsonObject();
            readTrees(root.getAsJsonArray("trees"), nextTrees);
            readCategories(root.getAsJsonArray("categories"), nextCategories);
            readNodes(root.getAsJsonArray("nodes"), nextNodes);
            readNetworks(root.getAsJsonArray("networks"), nextNetworks);
        });

        nextNodes.entrySet().removeIf(entry -> {
            ResearchNodeDefinition node = entry.getValue();
            return !nextTrees.containsKey(node.treeId()) || !nextCategories.containsKey(node.categoryId());
        });

        nextNodes.replaceAll((id, node) -> {
            List<ResourceLocation> prereqs = node.prerequisites().stream()
                    .filter(nextNodes::containsKey)
                    .toList();
            return new ResearchNodeDefinition(
                    node.id(),
                    node.treeId(),
                    node.categoryId(),
                    prereqs,
                    node.discoveryRules(),
                    node.researchCost(),
                    node.researchPower(),
                    node.researchTime(),
                    node.outputs()
            );
        });

        nextNetworks.replaceAll((id, network) -> {
            Set<ResourceLocation> filtered = network.nodeIds().stream().filter(nextNodes::containsKey).collect(HashSet::new, Set::add, Set::addAll);
            return new ResearchNetworkDefinition(network.id(), network.name(), Set.copyOf(filtered));
        });

        trees = Map.copyOf(nextTrees);
        categories = Map.copyOf(nextCategories);
        nodes = Map.copyOf(nextNodes);
        networks = Map.copyOf(nextNetworks);

        INCore.LOGGER.info("Loaded research v2 registry: {} trees, {} categories, {} nodes, {} networks.", trees.size(), categories.size(), nodes.size(), networks.size());
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

    public static Map<ResourceLocation, ResearchNetworkDefinition> networks() {
        return networks;
    }

    private static void readTrees(JsonArray treesArray, Map<ResourceLocation, ResearchTreeDefinition> output) {
        if (treesArray == null) {
            return;
        }

        for (JsonElement element : treesArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = parseId(row, "id");
            if (id == null) {
                continue;
            }
            String name = stringOr(row, "name", id.toString());
            String planetTheme = stringOr(row, "planet_theme", "");
            output.put(id, new ResearchTreeDefinition(id, name, planetTheme));
        }
    }

    private static void readCategories(JsonArray categoriesArray, Map<ResourceLocation, ResearchCategoryDefinition> output) {
        if (categoriesArray == null) {
            return;
        }

        for (JsonElement element : categoriesArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = parseId(row, "id");
            if (id == null) {
                continue;
            }
            String name = stringOr(row, "name", id.toString());
            ResourceLocation icon = parseId(row, "icon");
            output.put(id, new ResearchCategoryDefinition(id, name, icon));
        }
    }

    private static void readNodes(JsonArray nodesArray, Map<ResourceLocation, ResearchNodeDefinition> output) {
        if (nodesArray == null) {
            return;
        }

        for (JsonElement element : nodesArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = parseId(row, "id");
            ResourceLocation treeId = parseId(row, "tree_id");
            ResourceLocation categoryId = parseId(row, "category_id");
            if (id == null || treeId == null || categoryId == null) {
                continue;
            }

            List<ResourceLocation> prerequisites = parseIdList(row.getAsJsonArray("prerequisites"));
            String discoveryRules = row.has("discovery_rules") ? row.get("discovery_rules").getAsString() : null;
            int researchTime = row.has("research_time") ? Math.max(1, row.get("research_time").getAsInt()) : 200;
            ResearchCostDefinition cost = parseCost(row.getAsJsonObject("research_cost"));
            ResearchPowerDefinition power = parsePower(row.getAsJsonObject("research_power"));
            List<String> outputs = parseStringList(row.getAsJsonArray("outputs"));

            output.put(id, new ResearchNodeDefinition(id, treeId, categoryId, List.copyOf(prerequisites), discoveryRules, cost, power, researchTime, List.copyOf(outputs)));
        }
    }

    private static void readNetworks(JsonArray networksArray, Map<ResourceLocation, ResearchNetworkDefinition> output) {
        if (networksArray == null) {
            return;
        }

        for (JsonElement element : networksArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            ResourceLocation id = parseId(row, "id");
            if (id == null) {
                continue;
            }

            String name = stringOr(row, "name", id.toString());
            Set<ResourceLocation> nodeIds = Set.copyOf(parseIdList(row.getAsJsonArray("node_ids")));
            output.put(id, new ResearchNetworkDefinition(id, name, nodeIds));
        }
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
                int count = row.has("count") ? Math.max(0, row.get("count").getAsInt()) : 0;
                if (!tier.isBlank() && count > 0) {
                    logicModules.add(new ResearchCostDefinition.LogicModuleRequirement(tier, count));
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

    private static ResourceLocation parseId(JsonObject object, String key) {
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
}
