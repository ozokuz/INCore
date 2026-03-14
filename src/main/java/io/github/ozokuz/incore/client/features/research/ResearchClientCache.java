package io.github.ozokuz.incore.client.features.research;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ResearchClientCache {
    private static Snapshot snapshot = Snapshot.empty();
    private static String selectedTreeId = "";

    private ResearchClientCache() {
    }

    public static synchronized void updateFromJson(String json) {
        JsonObject root;
        try {
            root = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException ignored) {
            snapshot = Snapshot.empty();
            selectedTreeId = "";
            return;
        }

        String teamId = stringOr(root, "teamId", "");
        String activeNetworkId = stringOr(root, "activeNetworkId", "");
        boolean researchEnabled = boolOr(root, "researchEnabled", false);
        int stationNetworkCount = intOr(root, "stationNetworkCount", 0);
        boolean stationNetworkValid = boolOr(root, "stationNetworkValid", true);
        String stationNetworkStatus = stringOr(root, "stationNetworkStatus", "none");
        String stationNetworkWarning = stringOr(root, "stationNetworkWarning", "");
        int activeStationCount = intOr(root, "activeStationCount", 0);
        int linkedStationCount = intOr(root, "linkedStationCount", 0);
        int controllerTier = intOr(root, "controllerTier", 0);
        boolean focusModeEnabled = boolOr(root, "focusModeEnabled", false);
        int stationCount = intOr(root, "stationCount", 0);
        List<StationEntry> stations = readStations(root.getAsJsonArray("stations"));
        Set<String> discoveredNodeIds = readStringSet(root.getAsJsonArray("discoveredNodes"));
        Set<String> completedNodeIds = readStringSet(root.getAsJsonArray("completedNodes"));
        List<QueueEntry> researchQueue = readQueue(root.getAsJsonArray("researchQueue"));
        int storedResearchPowerBuffer = intOr(root, "storedResearchPowerBuffer", 0);
        int availableResearchPower = intOr(root, "availableResearchPower", 0);
        Map<String, Integer> devResearchMaterials = readStringIntMap(root.getAsJsonObject("devResearchMaterials"));
        Map<String, Integer> devLogicModules = readStringIntMap(root.getAsJsonObject("devLogicModules"));
        List<TreeEntry> trees = readTrees(root.getAsJsonArray("trees"));
        Map<String, CategoryEntry> categoriesById = readCategories(root.getAsJsonArray("categories"));
        List<NodeEntry> nodes = readNodes(root.getAsJsonArray("nodes"));

        Map<String, TreeEntry> treeById = new LinkedHashMap<>();
        for (TreeEntry tree : trees) {
            treeById.put(tree.id(), tree);
        }

        Map<String, NodeEntry> nodeById = new LinkedHashMap<>();
        for (NodeEntry node : nodes) {
            nodeById.put(node.id(), node);
        }

        snapshot = new Snapshot(
                true,
                teamId,
                activeNetworkId,
                researchEnabled,
                Math.max(0, stationNetworkCount),
                stationNetworkValid,
                stationNetworkStatus,
                stationNetworkWarning,
                Math.max(0, activeStationCount),
                Math.max(0, linkedStationCount),
                Math.max(0, controllerTier),
                focusModeEnabled,
                Math.max(0, stationCount),
                List.copyOf(stations),
                Set.copyOf(discoveredNodeIds),
                Set.copyOf(completedNodeIds),
                List.copyOf(researchQueue),
                Math.max(0, storedResearchPowerBuffer),
                Math.max(0, availableResearchPower),
                Map.copyOf(devResearchMaterials),
                Map.copyOf(devLogicModules),
                List.copyOf(trees),
                Map.copyOf(treeById),
                Map.copyOf(categoriesById),
                List.copyOf(nodes),
                Map.copyOf(nodeById)
        );

        if (selectedTreeId.isBlank() || !snapshot.treeById().containsKey(selectedTreeId)) {
            selectedTreeId = snapshot.trees().isEmpty() ? "" : snapshot.trees().get(0).id();
        }
    }

    public static synchronized Snapshot snapshot() {
        return snapshot;
    }

    public static synchronized String selectedTreeId() {
        return selectedTreeId;
    }

    public static synchronized void setSelectedTreeId(String treeId) {
        if (treeId == null || treeId.isBlank()) {
            return;
        }
        if (snapshot.treeById().containsKey(treeId)) {
            selectedTreeId = treeId;
        }
    }

    private static List<TreeEntry> readTrees(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<TreeEntry> trees = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject row = element.getAsJsonObject();
            String id = stringOr(row, "id", "");
            if (id.isBlank()) {
                continue;
            }
            String name = stringOr(row, "name", id);
            trees.add(new TreeEntry(id, name));
        }
        trees.sort(Comparator.comparing(TreeEntry::id));
        return trees;
    }

    private static Map<String, CategoryEntry> readCategories(JsonArray array) {
        if (array == null) {
            return Map.of();
        }

        Map<String, CategoryEntry> categories = new LinkedHashMap<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject row = element.getAsJsonObject();
            String id = stringOr(row, "id", "");
            if (id.isBlank()) {
                continue;
            }
            String name = stringOr(row, "name", id);
            String icon = stringOr(row, "icon", "");
            categories.put(id, new CategoryEntry(id, name, icon));
        }
        return categories;
    }

    private static List<NodeEntry> readNodes(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<NodeEntry> nodes = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject row = element.getAsJsonObject();
            String id = stringOr(row, "id", "");
            if (id.isBlank()) {
                continue;
            }

            nodes.add(new NodeEntry(
                    id,
                    stringOr(row, "name", id),
                    stringOr(row, "treeId", ""),
                    stringOr(row, "categoryId", ""),
                    List.copyOf(readStringSet(row.getAsJsonArray("prerequisites"))),
                    Math.max(1, intOr(row, "researchTime", 1)),
                    Math.max(1, intOr(row, "requiredRuns", 3)),
                    readLogicRequirements(row.getAsJsonArray("requiredLogicModules")),
                    readMaterialRequirements(row.getAsJsonArray("requiredResearchMaterials")),
                    List.copyOf(readStringSet(row.getAsJsonArray("outputs")))
            ));
        }

        nodes.sort(Comparator.comparing(NodeEntry::id));
        return nodes;
    }

    private static List<QueueEntry> readQueue(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<QueueEntry> queue = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String nodeId = stringOr(row, "nodeId", "");
            if (nodeId.isBlank()) {
                continue;
            }
            queue.add(new QueueEntry(
                    nodeId,
                    Math.max(0, intOr(row, "runTickProgress", 0)),
                    Math.max(1, intOr(row, "runTickRequired", 1)),
                    Math.max(0, intOr(row, "completedRuns", 0)),
                    Math.max(1, intOr(row, "requiredRuns", 1)),
                    boolOr(row, "runInputsCommitted", false),
                    stringOr(row, "status", "QUEUED"),
                    Math.max(0, intOr(row, "runPowerMultiplierBps", 10_000)),
                    Math.max(0, intOr(row, "runBonusRunChanceBps", 0)),
                    Math.max(0, intOr(row, "runCorruptionMultiplierBps", 10_000))
            ));
        }
        return queue;
    }

    private static List<StationEntry> readStations(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<StationEntry> stations = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String stationId = stringOr(row, "stationId", "");
            if (stationId.isBlank()) {
                continue;
            }

            JsonObject controllerPos = row.has("controllerPos") && row.get("controllerPos").isJsonObject()
                    ? row.getAsJsonObject("controllerPos")
                    : null;
            JsonObject endpoints = row.has("endpoints") && row.get("endpoints").isJsonObject()
                    ? row.getAsJsonObject("endpoints")
                    : null;
            JsonArray inputRows = endpoints != null && endpoints.has("inputs") && endpoints.get("inputs").isJsonArray()
                    ? endpoints.getAsJsonArray("inputs")
                    : null;
            JsonObject logicHousing = endpoints != null && endpoints.has("logicHousing") && endpoints.get("logicHousing").isJsonObject()
                    ? endpoints.getAsJsonObject("logicHousing")
                    : null;
            JsonObject researchDrive = endpoints != null && endpoints.has("researchDrive") && endpoints.get("researchDrive").isJsonObject()
                    ? endpoints.getAsJsonObject("researchDrive")
                    : null;
            JsonObject materialStorage = endpoints != null && endpoints.has("materialStorage") && endpoints.get("materialStorage").isJsonObject()
                    ? endpoints.getAsJsonObject("materialStorage")
                    : null;
            JsonArray outputPorts = endpoints != null && endpoints.has("outputPorts") && endpoints.get("outputPorts").isJsonArray()
                    ? endpoints.getAsJsonArray("outputPorts")
                    : null;
            JsonObject augmenter = endpoints != null && endpoints.has("augmenter") && endpoints.get("augmenter").isJsonObject()
                    ? endpoints.getAsJsonObject("augmenter")
                    : null;
            int x = intOr(controllerPos, "x", 0);
            int y = intOr(controllerPos, "y", 0);
            int z = intOr(controllerPos, "z", 0);

            stations.add(new StationEntry(
                    stationId,
                    boolOr(row, "formed", false),
                    Math.max(0, intOr(row, "stationTier", 0)),
                    Math.max(0, intOr(row, "rpBuffer", 0)),
                    Math.max(0, intOr(row, "rpCapacity", 0)),
                    stringOr(row, "powerFamily", ""),
                    Math.max(0, intOr(row, "powerInputTier", 0)),
                    Math.max(0, intOr(row, "connectedPartCount", 0)),
                    stringOr(row, "dimensionId", ""),
                    x,
                    y,
                    z,
                    readPositions(inputRows),
                    stringOr(row, "outputPortModes", "NONE"),
                    Math.max(0, intOr(row, "mountedDiskTier", 0)),
                    Math.max(0, intOr(row, "mountedDiskSnapshotCount", 0)),
                    Math.max(0, intOr(row, "mountedDiskCorruptedSegmentCount", 0)),
                    Math.max(0, intOr(row, "mountedDiskCorruptedSnapshotCount", 0)),
                    Math.max(0.0D, doubleOr(row, "activeSpeedMultiplier", 1.0D)),
                    Math.max(0.0D, doubleOr(row, "activePowerMultiplier", 1.0D)),
                    Math.max(0.0D, doubleOr(row, "activeBonusRunChance", 0.0D)),
                    Math.max(0.0D, doubleOr(row, "activeCorruptionMultiplier", 1.0D)),
                    readPosition(logicHousing),
                    readPosition(researchDrive),
                    readPosition(materialStorage),
                    readPositions(outputPorts),
                    readPosition(augmenter)
                    ,
                    stringOr(row, "stationNetworkId", ""),
                    boolOr(row, "singletonNetwork", false),
                    boolOr(row, "linked", false),
                    boolOr(row, "hasLinkPort", false)
            ));
        }
        stations.sort(Comparator.comparing(StationEntry::stationId));
        return List.copyOf(stations);
    }

    private static PositionEntry readPosition(JsonObject object) {
        if (object == null) {
            return new PositionEntry(0, 0, 0);
        }
        return new PositionEntry(intOr(object, "x", 0), intOr(object, "y", 0), intOr(object, "z", 0));
    }

    private static List<PositionEntry> readPositions(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<PositionEntry> positions = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            positions.add(new PositionEntry(
                    intOr(row, "x", 0),
                    intOr(row, "y", 0),
                    intOr(row, "z", 0)
            ));
        }
        return List.copyOf(positions);
    }

    private static List<LogicModuleRequirement> readLogicRequirements(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<LogicModuleRequirement> requirements = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String tier = stringOr(row, "moduleTier", "");
            int durabilityCost = Math.max(0, intOr(row, "durabilityCost", 0));
            if (!tier.isBlank() && durabilityCost > 0) {
                requirements.add(new LogicModuleRequirement(tier, durabilityCost));
            }
        }
        return List.copyOf(requirements);
    }

    private static List<ResearchMaterialRequirement> readMaterialRequirements(JsonArray array) {
        if (array == null) {
            return List.of();
        }

        List<ResearchMaterialRequirement> requirements = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject row = element.getAsJsonObject();
            String materialId = stringOr(row, "materialId", "");
            int count = Math.max(0, intOr(row, "count", 0));
            if (!materialId.isBlank() && count > 0) {
                requirements.add(new ResearchMaterialRequirement(materialId, count));
            }
        }
        return List.copyOf(requirements);
    }

    private static Set<String> readStringSet(JsonArray array) {
        if (array == null) {
            return Set.of();
        }

        Set<String> values = new LinkedHashSet<>();
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                continue;
            }
            String value = element.getAsString();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }

    private static Map<String, Integer> readStringIntMap(JsonObject object) {
        if (object == null) {
            return Map.of();
        }

        Map<String, Integer> values = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank() || !entry.getValue().isJsonPrimitive()) {
                continue;
            }

            int value = Math.max(0, entry.getValue().getAsInt());
            if (value > 0) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static String stringOr(JsonObject object, String key, String fallback) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static int intOr(JsonObject object, String key, int fallback) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static boolean boolOr(JsonObject object, String key, boolean fallback) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsBoolean();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private static double doubleOr(JsonObject object, String key, double fallback) {
        if (object == null || key == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }
        try {
            return object.get(key).getAsDouble();
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    public record Snapshot(
            boolean loaded,
            String teamId,
            String activeNetworkId,
            boolean researchEnabled,
            int stationNetworkCount,
            boolean stationNetworkValid,
            String stationNetworkStatus,
            String stationNetworkWarning,
            int activeStationCount,
            int linkedStationCount,
            int controllerTier,
            boolean focusModeEnabled,
            int stationCount,
            List<StationEntry> stations,
            Set<String> discoveredNodeIds,
            Set<String> completedNodeIds,
            List<QueueEntry> researchQueue,
            int storedResearchPowerBuffer,
            int availableResearchPower,
            Map<String, Integer> devResearchMaterials,
            Map<String, Integer> devLogicModules,
            List<TreeEntry> trees,
            Map<String, TreeEntry> treeById,
            Map<String, CategoryEntry> categoriesById,
            List<NodeEntry> nodes,
            Map<String, NodeEntry> nodeById
    ) {
        public static Snapshot empty() {
            return new Snapshot(
                    false,
                    "",
                    "",
                    false,
                    0,
                    true,
                    "none",
                    "",
                    0,
                    0,
                    0,
                    false,
                    0,
                    List.of(),
                    Set.of(),
                    Set.of(),
                    List.of(),
                    0,
                    0,
                    Map.of(),
                    Map.of(),
                    List.of(),
                    Map.of(),
                    Map.of(),
                    List.of(),
                    Map.of()
            );
        }
    }

    public record TreeEntry(String id, String name) {
    }

    public record CategoryEntry(String id, String name, String iconId) {
    }

    public record QueueEntry(
            String nodeId,
            int runTickProgress,
            int runTickRequired,
            int completedRuns,
            int requiredRuns,
            boolean runInputsCommitted,
            String status,
            int runPowerMultiplierBps,
            int runBonusRunChanceBps,
            int runCorruptionMultiplierBps
    ) {
    }

    public record NodeEntry(
            String id,
            String name,
            String treeId,
            String categoryId,
            List<String> prerequisites,
            int researchTime,
            int requiredRuns,
            List<LogicModuleRequirement> requiredLogicModules,
            List<ResearchMaterialRequirement> requiredResearchMaterials,
            List<String> outputs
    ) {
    }

    public record LogicModuleRequirement(String moduleTier, int durabilityCost) {
    }

    public record ResearchMaterialRequirement(String materialId, int count) {
    }

    public record StationEntry(
            String stationId,
            boolean formed,
            int stationTier,
            int rpBuffer,
            int rpCapacity,
            String powerFamily,
            int powerInputTier,
            int connectedPartCount,
            String dimensionId,
            int controllerX,
            int controllerY,
            int controllerZ,
            List<PositionEntry> inputPositions,
            String outputPortModes,
            int mountedDiskTier,
            int mountedDiskSnapshotCount,
            int mountedDiskCorruptedSegmentCount,
            int mountedDiskCorruptedSnapshotCount,
            double activeSpeedMultiplier,
            double activePowerMultiplier,
            double activeBonusRunChance,
            double activeCorruptionMultiplier,
            PositionEntry logicHousingPos,
            PositionEntry researchDrivePos,
            PositionEntry materialStoragePos,
            List<PositionEntry> outputPortPositions,
            PositionEntry augmenterPos,
            String stationNetworkId,
            boolean singletonNetwork,
            boolean linked,
            boolean hasLinkPort
    ) {
    }

    public record PositionEntry(int x, int y, int z) {
    }
}
