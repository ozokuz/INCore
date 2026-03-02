package io.github.ozokuz.incore.features.researchv2;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.ozokuz.incore.features.researchv2.event.ResearchV2LifecycleCallbacks;
import io.github.ozokuz.incore.features.researchv2.model.ResearchCostDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNetworkDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchNodeDefinition;
import io.github.ozokuz.incore.features.researchv2.model.ResearchPowerDefinition;
import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.provider.ResearchProviderManager;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.researchv2.state.ResearchQueueEntry;
import io.github.ozokuz.incore.features.researchv2.state.ResearchQueueStatus;
import io.github.ozokuz.incore.features.researchv2.state.TeamResearchState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ResearchManager {
    private static final Set<ResourceLocation> TIER0_CATEGORY_WHITELIST = Set.of(ResourceLocation.parse("incore:foundations"));

    private ResearchManager() {
    }

    public static boolean isDiscovered(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = getTeamState(server, teamId);
        return state != null && state.discoveredNodes().contains(nodeId);
    }

    public static boolean isResearched(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = getTeamState(server, teamId);
        return state != null && state.completedNodes().contains(nodeId);
    }

    public static boolean canDiscover(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        if (!hasValidNetwork(state)) {
            return false;
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null || !isNodeInActiveNetwork(state, nodeId)) {
            return false;
        }
        if (state.discoveredNodes().contains(nodeId) || state.completedNodes().contains(nodeId)) {
            return false;
        }
        return state.discoveredNodes().containsAll(node.prerequisites());
    }

    public static boolean canQueue(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        if (!hasValidNetwork(state) || !isNodeInActiveNetwork(state, nodeId)) {
            return false;
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null) {
            return false;
        }
        if (!isTier0BasicNode(node)) {
            return false;
        }

        if (!state.discoveredNodes().contains(nodeId) || state.completedNodes().contains(nodeId)) {
            return false;
        }
        if (!state.completedNodes().containsAll(node.prerequisites())) {
            return false;
        }
        if (state.researchQueue().stream().map(ResearchQueueEntry::nodeId).anyMatch(nodeId::equals)) {
            return false;
        }
        return true;
    }

    public static String explainQueueFailure(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = getTeamState(server, teamId);
        if (state == null) {
            return "missing team state";
        }
        if (!hasValidNetwork(state)) {
            return "team has no active research network";
        }
        if (!isNodeInActiveNetwork(state, nodeId)) {
            return "node is not in the team's active network";
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null) {
            return "unknown research node";
        }
        if (!isTier0BasicNode(node)) {
            return "node is not Tier 0 basic research";
        }
        if (!state.discoveredNodes().contains(nodeId)) {
            return "node is not discovered";
        }
        if (state.completedNodes().contains(nodeId)) {
            return "node is already completed";
        }
        if (!state.completedNodes().containsAll(node.prerequisites())) {
            return "prerequisites are not completed";
        }
        if (state.researchQueue().stream().map(ResearchQueueEntry::nodeId).anyMatch(nodeId::equals)) {
            return "node is already queued";
        }
        return "unknown reason";
    }

    public static boolean queueResearch(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        if (!canQueue(server, teamId, nodeId)) {
            return false;
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null) {
            return false;
        }

        ResearchNetworkSavedData data = ResearchNetworkSavedData.get(server);
        TeamResearchState state = ensureTeamState(server, teamId);
        state.researchQueue().add(new ResearchQueueEntry(
                nodeId,
                0,
                Math.max(1, node.researchTime()),
                0,
                Math.max(1, node.requiredRuns()),
                false,
                ResearchQueueStatus.QUEUED,
                List.of()
        ));
        data.setDirty();
        ResearchV2Networking.syncTeam(server, teamId);
        return true;
    }

    public static boolean tickResearch(MinecraftServer server, String teamId) {
        TeamResearchState state = getTeamState(server, teamId);
        if (state == null || !hasValidNetwork(state) || state.researchQueue().isEmpty()) {
            return false;
        }

        ResearchNetworkSavedData data = ResearchNetworkSavedData.get(server);
        ResearchQueueEntry head = state.researchQueue().get(0);
        ResourceLocation nodeId = head.nodeId();
        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);

        if (node == null || !isNodeInActiveNetwork(state, nodeId)) {
            state.researchQueue().remove(0);
            data.setDirty();
            return true;
        }

        if (state.completedNodes().contains(nodeId)) {
            state.researchQueue().remove(0);
            data.setDirty();
            return true;
        }

        int runTickRequired = resolveRunTickRequired(head, node);
        int runTickProgress = Math.min(runTickRequired, Math.max(0, head.runTickProgress()));
        int requiredRuns = resolveRequiredRuns(head, node);
        int completedRuns = Math.max(0, Math.min(head.completedRuns(), requiredRuns));
        boolean runInputsCommitted = head.runInputsCommitted();

        if (!state.discoveredNodes().contains(nodeId)
                || !state.completedNodes().containsAll(node.prerequisites())) {
            return updateQueueHead(
                    state,
                    node,
                    runTickProgress,
                    runTickRequired,
                    completedRuns,
                    requiredRuns,
                    runInputsCommitted,
                    ResearchQueueStatus.PAUSED_MISSING_INPUTS,
                    false,
                    data
            );
        }

        if (completedRuns >= requiredRuns) {
            state.discoveredNodes().add(nodeId);
            state.completedNodes().add(nodeId);
            state.researchQueue().remove(0);
            data.setDirty();
            ResearchV2LifecycleCallbacks.onResearchCompleted(teamId, nodeId);
            return true;
        }

        if (!runInputsCommitted) {
            ResearchCostDefinition cost = node.researchCost();
            if (!ResearchProviderManager.hasRequiredModules(server, teamId, cost)
                    || !ResearchProviderManager.hasRequiredMaterials(server, teamId, cost)
                    || !ResearchProviderManager.consumeRequiredModules(server, teamId, cost)
                    || !ResearchProviderManager.consumeRequiredMaterials(server, teamId, cost)) {
                return updateQueueHead(
                        state,
                        node,
                        runTickProgress,
                        runTickRequired,
                        completedRuns,
                        requiredRuns,
                        false,
                        ResearchQueueStatus.PAUSED_MISSING_INPUTS,
                        false,
                        data
                );
            }

            updateQueueHead(
                    state,
                    node,
                    runTickProgress,
                    runTickRequired,
                    completedRuns,
                    requiredRuns,
                    true,
                    ResearchQueueStatus.RUNNING,
                    true,
                    data
            );
            ResearchV2LifecycleCallbacks.onResearchStarted(teamId, nodeId, runTickRequired);
            runInputsCommitted = true;
        }

        int rpPerTick = computePowerCostPerTick(node.researchPower(), runTickProgress, runTickRequired);
        if (!ResearchProviderManager.consumePower(server, teamId, rpPerTick)) {
            return updateQueueHead(
                    state,
                    node,
                    runTickProgress,
                    runTickRequired,
                    completedRuns,
                    requiredRuns,
                    runInputsCommitted,
                    ResearchQueueStatus.PAUSED_NO_POWER,
                    false,
                    data
            );
        }

        int nextRunTickProgress = Math.min(runTickRequired, runTickProgress + 1);
        updateQueueHead(
                state,
                node,
                nextRunTickProgress,
                runTickRequired,
                completedRuns,
                requiredRuns,
                runInputsCommitted,
                ResearchQueueStatus.RUNNING,
                true,
                data
        );
        ResearchV2LifecycleCallbacks.onResearchProgress(teamId, nodeId, nextRunTickProgress, runTickRequired, rpPerTick);

        if (nextRunTickProgress >= runTickRequired) {
            int nextCompletedRuns = completedRuns + 1;
            if (nextCompletedRuns < requiredRuns) {
                return updateQueueHead(
                        state,
                        node,
                        0,
                        runTickRequired,
                        nextCompletedRuns,
                        requiredRuns,
                        false,
                        ResearchQueueStatus.QUEUED,
                        true,
                        data
                );
            }
            state.discoveredNodes().add(nodeId);
            state.completedNodes().add(nodeId);
            state.researchQueue().remove(0);
            data.setDirty();
            ResearchV2LifecycleCallbacks.onResearchCompleted(teamId, nodeId);
        }
        return true;
    }

    public static boolean grantDiscovery(MinecraftServer server, String teamId, ResourceLocation nodeId, String reason) {
        if (!canDiscover(server, teamId, nodeId)) {
            return false;
        }

        TeamResearchState state = ensureTeamState(server, teamId);
        boolean changed = state.discoveredNodes().add(nodeId);
        if (changed) {
            ResearchNetworkSavedData.get(server).setDirty();
            ResearchV2Networking.syncTeam(server, teamId);
        }
        return changed;
    }

    public static boolean grantCompletion(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        if (!hasValidNetwork(state) || !isNodeInActiveNetwork(state, nodeId)) {
            return false;
        }
        if (!ResearchRegistry.nodes().containsKey(nodeId)) {
            return false;
        }

        boolean changed = false;
        changed = state.discoveredNodes().add(nodeId) || changed;
        changed = state.completedNodes().add(nodeId) || changed;
        changed = state.researchQueue().removeIf(entry -> nodeId.equals(entry.nodeId())) || changed;

        if (changed) {
            ResearchNetworkSavedData.get(server).setDirty();
            ResearchV2Networking.syncTeam(server, teamId);
        }
        return changed;
    }

    public static boolean cancelQueuedResearchCascade(MinecraftServer server, String teamId, ResourceLocation nodeId) {
        TeamResearchState state = getTeamState(server, teamId);
        if (state == null) {
            return false;
        }

        boolean hasTarget = state.researchQueue().stream().anyMatch(entry -> nodeId.equals(entry.nodeId()));
        if (!hasTarget) {
            return false;
        }

        Set<ResourceLocation> cancelledNodeIds = new HashSet<>();
        Deque<ResourceLocation> queue = new ArrayDeque<>();
        queue.add(nodeId);

        while (!queue.isEmpty()) {
            ResourceLocation cancelledNodeId = queue.removeFirst();
            if (!cancelledNodeIds.add(cancelledNodeId)) {
                continue;
            }

            for (ResearchQueueEntry entry : state.researchQueue()) {
                ResourceLocation candidateId = entry.nodeId();
                if (cancelledNodeIds.contains(candidateId)) {
                    continue;
                }

                ResearchNodeDefinition node = ResearchRegistry.nodes().get(candidateId);
                if (node != null && node.prerequisites().contains(cancelledNodeId)) {
                    queue.addLast(candidateId);
                }
            }
        }

        boolean changed = state.researchQueue().removeIf(entry -> cancelledNodeIds.contains(entry.nodeId()));
        if (changed) {
            ResearchNetworkSavedData.get(server).setDirty();
            ResearchV2Networking.syncTeam(server, teamId);
        }
        return changed;
    }

    public static boolean setControllerTier(MinecraftServer server, String teamId, int controllerTier) {
        TeamResearchState state = ensureTeamState(server, teamId);
        int nextControllerTier = Math.max(0, controllerTier);
        if (state.controllerTier() == nextControllerTier) {
            return false;
        }

        state.setControllerTier(nextControllerTier);
        ResearchNetworkSavedData.get(server).setDirty();
        ResearchV2Networking.syncTeam(server, teamId);
        return true;
    }

    public static boolean clearResearch(MinecraftServer server, String teamId) {
        TeamResearchState state = getTeamState(server, teamId);
        if (state == null) {
            return false;
        }

        boolean changed = !state.discoveredNodes().isEmpty()
                || !state.completedNodes().isEmpty()
                || !state.researchQueue().isEmpty()
                || state.storedResearchPowerBuffer() > 0
                || state.controllerTier() > 0
                || !state.devResearchMaterials().isEmpty()
                || !state.devLogicModules().isEmpty();

        state.discoveredNodes().clear();
        state.completedNodes().clear();
        state.researchQueue().clear();
        state.setStoredResearchPowerBuffer(0);
        state.setControllerTier(0);
        state.devResearchMaterials().clear();
        state.devLogicModules().clear();

        if (changed) {
            ResearchNetworkSavedData.get(server).setDirty();
            ResearchV2Networking.syncTeam(server, teamId);
        }
        return changed;
    }

    public static String snapshotJson(MinecraftServer server, String teamId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        JsonObject root = new JsonObject();
        root.addProperty("teamId", teamId);
        root.addProperty("activeNetworkId", state.activeNetworkId() == null ? "" : state.activeNetworkId().toString());
        root.addProperty("researchEnabled", hasValidNetwork(state));
        root.addProperty("controllerTier", state.controllerTier());

        JsonArray discovered = new JsonArray();
        state.discoveredNodes().stream().map(ResourceLocation::toString).sorted().forEach(discovered::add);
        root.add("discoveredNodes", discovered);

        JsonArray completed = new JsonArray();
        state.completedNodes().stream().map(ResourceLocation::toString).sorted().forEach(completed::add);
        root.add("completedNodes", completed);

        JsonArray queue = new JsonArray();
        state.researchQueue().forEach(entry -> {
            JsonObject row = new JsonObject();
            row.addProperty("nodeId", entry.nodeId().toString());
            row.addProperty("runTickProgress", entry.runTickProgress());
            row.addProperty("runTickRequired", entry.runTickRequired());
            row.addProperty("completedRuns", entry.completedRuns());
            row.addProperty("requiredRuns", entry.requiredRuns());
            row.addProperty("runInputsCommitted", entry.runInputsCommitted());
            row.addProperty("status", entry.status().name());
            JsonArray stations = new JsonArray();
            entry.assignedStationIds().forEach(stations::add);
            row.add("assignedStationIds", stations);
            queue.add(row);
        });
        root.add("researchQueue", queue);

        root.addProperty("storedResearchPowerBuffer", state.storedResearchPowerBuffer());
        root.addProperty("availableResearchPower", ResearchProviderManager.availablePower(server, teamId));

        JsonObject materials = new JsonObject();
        state.devResearchMaterials().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> materials.addProperty(entry.getKey(), entry.getValue()));
        root.add("devResearchMaterials", materials);

        JsonObject modules = new JsonObject();
        state.devLogicModules().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> modules.addProperty(entry.getKey(), entry.getValue()));
        root.add("devLogicModules", modules);

        JsonArray trees = new JsonArray();
        ResearchRegistry.trees().values().stream()
                .sorted(Comparator.comparing(tree -> tree.id().toString()))
                .forEach(tree -> {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", tree.id().toString());
                    row.addProperty("name", tree.name());
                    trees.add(row);
                });
        root.add("trees", trees);

        JsonArray categories = new JsonArray();
        ResearchRegistry.categories().values().stream()
                .sorted(Comparator.comparing(category -> category.id().toString()))
                .forEach(category -> {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", category.id().toString());
                    row.addProperty("name", category.name());
                    row.addProperty("icon", category.icon() == null ? "" : category.icon().toString());
                    categories.add(row);
                });
        root.add("categories", categories);

        JsonArray nodes = new JsonArray();
        ResearchRegistry.nodes().values().stream()
                .sorted(Comparator.comparing(node -> node.id().toString()))
                .forEach(node -> {
                    JsonObject row = new JsonObject();
                    row.addProperty("id", node.id().toString());
                    row.addProperty("name", node.name());
                    row.addProperty("treeId", node.treeId().toString());
                    row.addProperty("categoryId", node.categoryId().toString());
                    JsonArray prerequisites = new JsonArray();
                    node.prerequisites().stream().map(ResourceLocation::toString).sorted().forEach(prerequisites::add);
                    row.add("prerequisites", prerequisites);
                    row.addProperty("researchTime", node.researchTime());
                    row.addProperty("requiredRuns", node.requiredRuns());

                    JsonArray requiredLogicModules = new JsonArray();
                    node.researchCost().requiredLogicModules().forEach(requirement -> {
                        JsonObject requirementObject = new JsonObject();
                        requirementObject.addProperty("moduleTier", requirement.moduleTier());
                        requirementObject.addProperty("durabilityCost", requirement.durabilityCost());
                        requiredLogicModules.add(requirementObject);
                    });
                    row.add("requiredLogicModules", requiredLogicModules);

                    JsonArray requiredResearchMaterials = new JsonArray();
                    node.researchCost().requiredResearchMaterials().forEach(requirement -> {
                        JsonObject requirementObject = new JsonObject();
                        requirementObject.addProperty("materialId", requirement.materialId());
                        requirementObject.addProperty("count", requirement.count());
                        requiredResearchMaterials.add(requirementObject);
                    });
                    row.add("requiredResearchMaterials", requiredResearchMaterials);

                    JsonArray outputs = new JsonArray();
                    node.outputs().forEach(outputs::add);
                    row.add("outputs", outputs);
                    nodes.add(row);
                });
        root.add("nodes", nodes);
        return root.toString();
    }

    public static TeamResearchState ensureTeamState(MinecraftServer server, String teamId) {
        ResearchNetworkSavedData data = ResearchNetworkSavedData.get(server);
        TeamResearchState state = data.getOrCreateTeamState(teamId);

        ResourceLocation current = state.activeNetworkId();
        if (current != null && ResearchRegistry.networks().containsKey(current)) {
            return state;
        }

        if (ResearchRegistry.networks().size() == 1) {
            ResourceLocation single = ResearchRegistry.networks().keySet().iterator().next();
            if (!Objects.equals(single, current)) {
                state.setActiveNetworkId(single);
                data.setDirty();
            }
        }
        return state;
    }

    private static TeamResearchState getTeamState(MinecraftServer server, String teamId) {
        if (teamId == null || teamId.isBlank()) {
            return null;
        }
        return ResearchNetworkSavedData.get(server).getTeamState(teamId);
    }

    private static boolean hasValidNetwork(TeamResearchState state) {
        ResourceLocation activeNetworkId = state.activeNetworkId();
        if (activeNetworkId == null) {
            return false;
        }
        return ResearchRegistry.networks().containsKey(activeNetworkId);
    }

    private static boolean isNodeInActiveNetwork(TeamResearchState state, ResourceLocation nodeId) {
        ResourceLocation activeNetworkId = state.activeNetworkId();
        if (activeNetworkId == null) {
            return false;
        }

        ResearchNetworkDefinition network = ResearchRegistry.networks().get(activeNetworkId);
        if (network == null) {
            return false;
        }
        Set<ResourceLocation> nodes = network.nodeIds();
        return nodes.contains(nodeId);
    }

    private static boolean isTier0BasicNode(ResearchNodeDefinition node) {
        return node != null && TIER0_CATEGORY_WHITELIST.contains(node.categoryId());
    }

    private static int resolveRunTickRequired(ResearchQueueEntry entry, ResearchNodeDefinition node) {
        if (entry.runTickRequired() > 0) {
            return entry.runTickRequired();
        }
        return Math.max(1, node.researchTime());
    }

    private static int resolveRequiredRuns(ResearchQueueEntry entry, ResearchNodeDefinition node) {
        if (entry.requiredRuns() > 0) {
            return entry.requiredRuns();
        }
        return Math.max(1, node.requiredRuns());
    }

    private static int computePowerCostPerTick(ResearchPowerDefinition power, int timeProgress, int requiredTime) {
        if (power == null) {
            power = ResearchPowerDefinition.defaults();
        }
        double ratio = requiredTime <= 0 ? 0.0D : Math.max(0.0D, (double) timeProgress / (double) requiredTime);
        double value = power.baseRpPerTick() + (power.curveScaleRpPerTick() * Math.pow(ratio, power.curveExponent()));
        return Math.max(0, (int) Math.ceil(value));
    }

    private static boolean updateQueueHead(
            TeamResearchState state,
            ResearchNodeDefinition node,
            int runTickProgress,
            int runTickRequired,
            int completedRuns,
            int requiredRuns,
            boolean runInputsCommitted,
            ResearchQueueStatus status,
            boolean forceDirty,
            ResearchNetworkSavedData data
    ) {
        if (state.researchQueue().isEmpty()) {
            return false;
        }

        ResearchQueueEntry existing = state.researchQueue().get(0);
        int normalizedRunTickRequired = runTickRequired <= 0 ? Math.max(1, node.researchTime()) : runTickRequired;
        int normalizedRunTickProgress = Math.max(0, Math.min(runTickProgress, normalizedRunTickRequired));
        int normalizedRequiredRuns = requiredRuns <= 0 ? Math.max(1, node.requiredRuns()) : requiredRuns;
        int normalizedCompletedRuns = Math.max(0, Math.min(completedRuns, normalizedRequiredRuns));
        ResearchQueueEntry replacement = new ResearchQueueEntry(
                existing.nodeId(),
                normalizedRunTickProgress,
                normalizedRunTickRequired,
                normalizedCompletedRuns,
                normalizedRequiredRuns,
                runInputsCommitted,
                status,
                existing.assignedStationIds()
        );

        boolean changed = forceDirty || !existing.equals(replacement);
        if (changed) {
            state.researchQueue().set(0, replacement);
            data.setDirty();
        }
        return changed;
    }
}
