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
import io.github.ozokuz.incore.features.researchv2.station.ResearchMultiblockStationRegistry;
import io.github.ozokuz.incore.features.researchv2.station.ResearchStationDescriptor;
import io.github.ozokuz.incore.features.researchv2.station.ResearchStationRuntime;
import io.github.ozokuz.incore.features.researchv2.station.ResearchStationAugmentSummary;
import io.github.ozokuz.incore.features.researchv2.station.ResearchControllerBlockEntity;
import io.github.ozokuz.incore.features.researchv2.station.ResearchStationServices;
import io.github.ozokuz.incore.features.researchv2.station.network.StationNetworkService;
import io.github.ozokuz.incore.features.researchv2.station.network.TeamStationNetworkSnapshot;
import net.minecraft.core.BlockPos;
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
        if (hasStationNetworkConflict(server, teamId)) {
            return false;
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null) {
            return false;
        }
        int controllerTier = effectiveControllerTier(server, teamId, state);
        if (!isNodeAllowedByControllerTier(node, controllerTier)) {
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
        if (hasStationNetworkConflict(server, teamId)) {
            return "team has multiple unlinked research station networks";
        }
        if (!isNodeInActiveNetwork(state, nodeId)) {
            return "node is not in the team's active network";
        }

        ResearchNodeDefinition node = ResearchRegistry.nodes().get(nodeId);
        if (node == null) {
            return "unknown research node";
        }
        int controllerTier = effectiveControllerTier(server, teamId, state);
        if (!isNodeAllowedByControllerTier(node, controllerTier)) {
            if (controllerTier <= 0) {
                return "team has no active research controller tier";
            }
            if (controllerTier == 1 && !isBasicCategory(node.categoryId())) {
                return "node requires controller tier 2";
            }
            return "node is not allowed by current controller tier";
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
        ResearchControllerBlockEntity executor = StationNetworkService.resolveAssignedExecutor(server, teamId, List.of(), false);
        List<String> assignedStations = executor == null || executor.stationId().isBlank()
                ? List.of()
                : List.of(executor.stationId());
        state.researchQueue().add(new ResearchQueueEntry(
                nodeId,
                0,
                Math.max(1, node.researchTime()),
                0,
                Math.max(1, node.requiredRuns()),
                false,
                ResearchQueueStatus.QUEUED,
                10_000,
                0,
                10_000,
                assignedStations
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
        ResearchControllerBlockEntity previouslyAssignedController = resolveStoredAssignedController(server, teamId, head.assignedStationIds());

        if (node == null || !isNodeInActiveNetwork(state, nodeId)) {
            if (previouslyAssignedController != null) {
                ResearchStationRuntime.setDiskLocked(previouslyAssignedController, false);
            }
            state.researchQueue().remove(0);
            data.setDirty();
            return true;
        }

        if (state.completedNodes().contains(nodeId)) {
            if (previouslyAssignedController != null) {
                ResearchStationRuntime.setDiskLocked(previouslyAssignedController, false);
            }
            state.researchQueue().remove(0);
            data.setDirty();
            return true;
        }

        TeamStationNetworkSnapshot networkSnapshot = StationNetworkService.snapshot(server, teamId);
        int runTickRequired = resolveRunTickRequired(head, node);
        int runTickProgress = Math.min(runTickRequired, Math.max(0, head.runTickProgress()));
        int requiredRuns = resolveRequiredRuns(head, node);
        int completedRuns = Math.max(0, Math.min(head.completedRuns(), requiredRuns));
        boolean runInputsCommitted = head.runInputsCommitted();
        if (!networkSnapshot.stationNetworkValid()) {
            if (previouslyAssignedController != null) {
                ResearchStationRuntime.setDiskLocked(previouslyAssignedController, false);
            }
            return updateQueueHead(
                    state,
                    node,
                    runTickProgress,
                    runTickRequired,
                    completedRuns,
                    requiredRuns,
                    runInputsCommitted,
                    ResearchQueueStatus.PAUSED_NETWORK_CONFLICT,
                    false,
                    data,
                    modifiersFromEntry(head),
                    head.assignedStationIds()
            );
        }

        List<ResearchControllerBlockEntity> executableControllers = StationNetworkService.executableControllers(server, teamId);
        ResearchControllerBlockEntity assignedController = StationNetworkService.resolveAssignedExecutor(server, teamId, head.assignedStationIds(), false);
        List<String> assignedStations = assignedController == null ? List.of() : List.of(assignedController.stationId());
        if (previouslyAssignedController != null && previouslyAssignedController != assignedController) {
            ResearchStationRuntime.setDiskLocked(previouslyAssignedController, false);
        }
        if (!assignedStations.equals(head.assignedStationIds())) {
            updateQueueHead(
                    state,
                    node,
                    runTickProgress,
                    runTickRequired,
                    completedRuns,
                    requiredRuns,
                    runInputsCommitted,
                    head.status(),
                    true,
                    data,
                    new RunModifierState(head.runPowerMultiplierBps(), head.runBonusRunChanceBps(), head.runCorruptionMultiplierBps()),
                    assignedStations
            );
            head = state.researchQueue().get(0);
        }
        if (assignedController != null) {
            ResearchStationRuntime.setDiskLocked(assignedController, true);
        }

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
                    data,
                    modifiersFromEntry(head),
                    assignedStations
            );
        }

        if (completedRuns >= requiredRuns) {
            if (assignedController != null) {
                ResearchStationRuntime.setDiskLocked(assignedController, false);
            }
            state.discoveredNodes().add(nodeId);
            state.completedNodes().add(nodeId);
            state.researchQueue().remove(0);
            data.setDirty();
            ResearchV2LifecycleCallbacks.onResearchCompleted(teamId, nodeId);
            return true;
        }

        if (!runInputsCommitted) {
            ResearchCostDefinition cost = node.researchCost();
            RunModifierState modifiers = assignedController == null
                    ? modifiersFromEntry(head)
                    : resolveRunModifiers(assignedController, node);
            int adjustedRunTickRequired = assignedController == null
                    ? runTickRequired
                    : Math.max(1, (int) Math.ceil(Math.max(1, node.researchTime()) * modifiers.speedMultiplier()));

            boolean canCommitInputs;
            if (!executableControllers.isEmpty()) {
                canCommitInputs = assignedController != null
                        && ResearchStationRuntime.hasWritableDisk(assignedController)
                        && ResearchStationRuntime.hasRequiredModules(executableControllers, cost.requiredLogicModules())
                        && ResearchStationRuntime.hasRequiredMaterials(executableControllers, cost.requiredResearchMaterials())
                        && ResearchStationRuntime.consumeRequiredModules(executableControllers, assignedController, nodeId, completedRuns, cost.requiredLogicModules())
                        && ResearchStationRuntime.consumeRequiredMaterials(executableControllers, cost.requiredResearchMaterials());
            } else {
                canCommitInputs = ResearchProviderManager.hasRequiredModules(server, teamId, cost)
                        && ResearchProviderManager.hasRequiredMaterials(server, teamId, cost)
                        && ResearchProviderManager.consumeRequiredModules(server, teamId, cost)
                        && ResearchProviderManager.consumeRequiredMaterials(server, teamId, cost);
            }

            if (!canCommitInputs) {
                return updateQueueHead(
                        state,
                        node,
                        runTickProgress,
                        adjustedRunTickRequired,
                        completedRuns,
                        requiredRuns,
                        false,
                        ResearchQueueStatus.PAUSED_MISSING_INPUTS,
                        false,
                        data,
                        modifiers,
                        assignedStations
                );
            }

            updateQueueHead(
                    state,
                    node,
                    runTickProgress,
                    adjustedRunTickRequired,
                    completedRuns,
                    requiredRuns,
                    true,
                    ResearchQueueStatus.RUNNING,
                    true,
                    data,
                    modifiers,
                    assignedStations
            );
            runTickRequired = adjustedRunTickRequired;
            ResearchV2LifecycleCallbacks.onResearchStarted(teamId, nodeId, adjustedRunTickRequired);
            runInputsCommitted = true;
            head = state.researchQueue().get(0);
        }

        RunModifierState activeModifiers = modifiersFromEntry(head);
        int rpPerTick = computePowerCostPerTick(node.researchPower(), runTickProgress, runTickRequired, activeModifiers.powerMultiplier());
        boolean consumedPower = ResearchProviderManager.consumePower(server, teamId, rpPerTick);
        if (!consumedPower) {
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
                    data,
                    activeModifiers,
                    assignedStations
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
                data,
                activeModifiers,
                assignedStations
        );
        ResearchV2LifecycleCallbacks.onResearchProgress(teamId, nodeId, nextRunTickProgress, runTickRequired, rpPerTick);

        if (nextRunTickProgress >= runTickRequired) {
            int nextCompletedRuns = completedRuns + 1;
            if (assignedController != null) {
                ResearchStationRuntime.writeDiskSnapshot(
                        assignedController,
                        nodeId,
                        nextCompletedRuns,
                        requiredRuns,
                        nextCompletedRuns,
                        false,
                        activeModifiers.corruptionMultiplier()
                );
                if (ResearchDeterministicRng.rollChance(
                        teamId,
                        assignedController.stationId(),
                        nodeId,
                        nextCompletedRuns,
                        "bonus_run",
                        activeModifiers.bonusRunChance()
                )) {
                    nextCompletedRuns = Math.min(requiredRuns, nextCompletedRuns + 1);
                }
            }
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
                        data,
                        RunModifierState.DEFAULT,
                        assignedStations
                );
            }
            if (assignedController != null) {
                ResearchStationRuntime.writeDiskSnapshot(
                        assignedController,
                        nodeId,
                        nextCompletedRuns,
                        requiredRuns,
                        nextCompletedRuns + 1,
                        true,
                        activeModifiers.corruptionMultiplier()
                );
                ResearchStationRuntime.setDiskLocked(assignedController, false);
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
        List<ResearchQueueEntry> removedEntries = state.researchQueue().stream().filter(entry -> nodeId.equals(entry.nodeId())).toList();
        unlockEntries(server, teamId, removedEntries);
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

        List<ResearchQueueEntry> removedEntries = state.researchQueue().stream().filter(entry -> cancelledNodeIds.contains(entry.nodeId())).toList();
        unlockEntries(server, teamId, removedEntries);
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

    public static int effectiveControllerTier(MinecraftServer server, String teamId) {
        TeamResearchState state = ensureTeamState(server, teamId);
        return effectiveControllerTier(server, teamId, state);
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

        unlockEntries(server, teamId, List.copyOf(state.researchQueue()));
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
        TeamStationNetworkSnapshot stationNetworkSnapshot = StationNetworkService.snapshot(server, teamId);
        int effectiveControllerTier = effectiveControllerTier(server, teamId, state);
        List<ResearchStationDescriptor> stations = ResearchMultiblockStationRegistry.stationsForTeam(server, teamId).stream()
                .map(station -> {
                    String stationNetworkId = stationNetworkSnapshot.stationNetworkIdsByStationId().getOrDefault(station.stationId(), "");
                    boolean linked = stationNetworkSnapshot.linkedStationIds().contains(station.stationId());
                    boolean hasLinkPort = stationNetworkSnapshot.stationsWithLinkPort().contains(station.stationId());
                    return station.withStationNetwork(stationNetworkId, !linked, linked, hasLinkPort);
                })
                .toList();

        JsonObject root = new JsonObject();
        root.addProperty("teamId", teamId);
        root.addProperty("activeNetworkId", state.activeNetworkId() == null ? "" : state.activeNetworkId().toString());
        root.addProperty("researchEnabled", hasValidNetwork(state) && stationNetworkSnapshot.stationNetworkValid());
        root.addProperty("stationNetworkCount", stationNetworkSnapshot.stationNetworkCount());
        root.addProperty("stationNetworkValid", stationNetworkSnapshot.stationNetworkValid());
        root.addProperty("stationNetworkStatus", stationNetworkSnapshot.stationNetworkStatus());
        root.addProperty("stationNetworkWarning", stationNetworkSnapshot.stationNetworkWarning());
        root.addProperty("activeStationCount", stationNetworkSnapshot.activeStationCount());
        root.addProperty("linkedStationCount", stationNetworkSnapshot.linkedStationCount());
        root.addProperty("controllerTier", effectiveControllerTier);
        root.addProperty("focusModeEnabled", effectiveControllerTier >= 3);
        root.addProperty("stationCount", stations.size());

        JsonArray stationRows = new JsonArray();
        for (ResearchStationDescriptor station : stations) {
            JsonObject row = new JsonObject();
            row.addProperty("stationId", station.stationId());
            row.addProperty("teamId", station.teamId());
            row.addProperty("dimensionId", station.dimensionId());
            row.addProperty("stationTier", station.stationTier());
            row.addProperty("formed", station.formed());
            row.addProperty("rpBuffer", station.rpBuffer());
            row.addProperty("rpCapacity", station.rpCapacity());
            row.addProperty("availableResearchPower", station.availableResearchPower());
            row.addProperty("slotCapacity", station.slotCapacity());
            row.addProperty("powerFamily", station.powerFamily() == null ? "" : station.powerFamily().name());
            row.addProperty("powerInputTier", station.powerInputTier());
            row.addProperty("outputPortModes", station.outputPortModes());
            row.addProperty("mountedDiskTier", station.mountedDiskTier());
            row.addProperty("mountedDiskSnapshotCount", station.mountedDiskSnapshotCount());
            row.addProperty("mountedDiskCorruptedSegmentCount", station.mountedDiskCorruptedSegmentCount());
            row.addProperty("mountedDiskCorruptedSnapshotCount", station.mountedDiskCorruptedSnapshotCount());
            row.addProperty("activeSpeedMultiplier", station.activeSpeedMultiplier());
            row.addProperty("activePowerMultiplier", station.activePowerMultiplier());
            row.addProperty("activeBonusRunChance", station.activeBonusRunChance());
            row.addProperty("activeCorruptionMultiplier", station.activeCorruptionMultiplier());
            row.addProperty("stationNetworkId", station.stationNetworkId());
            row.addProperty("singletonNetwork", station.singletonNetwork());
            row.addProperty("linked", station.linked());
            row.addProperty("hasLinkPort", station.hasLinkPort());
            row.add("controllerPos", toJsonPos(station.controllerPos()));

            JsonObject endpoints = new JsonObject();
            JsonArray inputRows = new JsonArray();
            station.endpoints().inputs().forEach(pos -> inputRows.add(toJsonPos(pos)));
            endpoints.add("inputs", inputRows);

            JsonArray inventoryRows = new JsonArray();
            station.endpoints().inventories().forEach(pos -> inventoryRows.add(toJsonPos(pos)));
            endpoints.add("inventories", inventoryRows);
            endpoints.add("logicHousing", toJsonPos(station.endpoints().logicHousing()));
            endpoints.add("researchDrive", toJsonPos(station.endpoints().researchDrive()));
            endpoints.add("materialStorage", toJsonPos(station.endpoints().materialStorage()));
            JsonArray outputPortRows = new JsonArray();
            station.endpoints().outputPorts().forEach(pos -> outputPortRows.add(toJsonPos(pos)));
            endpoints.add("outputPorts", outputPortRows);
            endpoints.add("augmenter", toJsonPos(station.endpoints().augmenter()));
            row.add("endpoints", endpoints);

            JsonArray connectedRows = new JsonArray();
            station.connectedParts().forEach(pos -> connectedRows.add(toJsonPos(pos)));
            row.add("connectedParts", connectedRows);
            row.addProperty("connectedPartCount", station.connectedParts().size());
            stationRows.add(row);
        }
        root.add("stations", stationRows);

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
            row.addProperty("runPowerMultiplierBps", entry.runPowerMultiplierBps());
            row.addProperty("runBonusRunChanceBps", entry.runBonusRunChanceBps());
            row.addProperty("runCorruptionMultiplierBps", entry.runCorruptionMultiplierBps());
            JsonArray assignedStations = new JsonArray();
            entry.assignedStationIds().forEach(assignedStations::add);
            row.add("assignedStationIds", assignedStations);
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

    private static boolean hasStationNetworkConflict(MinecraftServer server, String teamId) {
        return StationNetworkService.snapshot(server, teamId).stationNetworkCount() > 1;
    }

    private static boolean isBasicCategory(ResourceLocation categoryId) {
        return categoryId != null && TIER0_CATEGORY_WHITELIST.contains(categoryId);
    }

    private static boolean isNodeAllowedByControllerTier(ResearchNodeDefinition node, int controllerTier) {
        if (node == null || controllerTier <= 0) {
            return false;
        }
        if (controllerTier == 1) {
            return isBasicCategory(node.categoryId());
        }
        return true;
    }

    private static int effectiveControllerTier(MinecraftServer server, String teamId, TeamResearchState state) {
        int tier = state == null ? 0 : Math.max(0, state.controllerTier());
        for (ResearchStationDescriptor descriptor : ResearchMultiblockStationRegistry.stationsForTeam(server, teamId)) {
            if (!descriptor.formed()) {
                continue;
            }
            tier = Math.max(tier, descriptor.stationTier());
        }
        return tier;
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

    private static int computePowerCostPerTick(ResearchPowerDefinition power, int timeProgress, int requiredTime, double powerMultiplier) {
        if (power == null) {
            power = ResearchPowerDefinition.defaults();
        }
        double ratio = requiredTime <= 0 ? 0.0D : Math.max(0.0D, (double) timeProgress / (double) requiredTime);
        double value = (power.baseRpPerTick() + (power.curveScaleRpPerTick() * Math.pow(ratio, power.curveExponent()))) * Math.max(0.0D, powerMultiplier);
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
            ResearchNetworkSavedData data,
            RunModifierState modifiers,
            List<String> assignedStations
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
                modifiers.powerMultiplierBps(),
                modifiers.bonusRunChanceBps(),
                modifiers.corruptionMultiplierBps(),
                assignedStations == null ? existing.assignedStationIds() : List.copyOf(assignedStations)
        );

        boolean changed = forceDirty || !existing.equals(replacement);
        if (changed) {
            state.researchQueue().set(0, replacement);
            data.setDirty();
        }
        return changed;
    }

    private static RunModifierState modifiersFromEntry(ResearchQueueEntry entry) {
        if (entry == null) {
            return RunModifierState.DEFAULT;
        }
        return new RunModifierState(
                entry.runPowerMultiplierBps() <= 0 ? 10_000 : entry.runPowerMultiplierBps(),
                Math.max(0, entry.runBonusRunChanceBps()),
                entry.runCorruptionMultiplierBps() <= 0 ? 10_000 : entry.runCorruptionMultiplierBps()
        );
    }

    private static RunModifierState resolveRunModifiers(ResearchControllerBlockEntity controller, ResearchNodeDefinition node) {
        ResearchStationAugmentSummary summary = controller == null
                ? ResearchStationAugmentSummary.DEFAULT
                : ResearchStationServices.computeAugmentSummary(controller.getLevel(), controller, node.categoryId());
        int powerMultiplierBps = Math.max(1, (int) Math.round(summary.powerMultiplier() * 10_000.0D));
        int bonusRunChanceBps = Math.max(0, Math.min(9_000, (int) Math.round(summary.bonusRunChance() * 10_000.0D)));
        int corruptionMultiplierBps = Math.max(1, (int) Math.round(summary.corruptionMultiplier() * 10_000.0D));
        return new RunModifierState(powerMultiplierBps, bonusRunChanceBps, corruptionMultiplierBps, summary.speedMultiplier());
    }

    private static void unlockEntries(MinecraftServer server, String teamId, List<ResearchQueueEntry> entries) {
        if (entries == null) {
            return;
        }
        for (ResearchQueueEntry entry : entries) {
            ResearchControllerBlockEntity controller = resolveStoredAssignedController(server, teamId, entry.assignedStationIds());
            if (controller != null) {
                ResearchStationRuntime.setDiskLocked(controller, false);
            }
        }
    }

    private static ResearchControllerBlockEntity resolveStoredAssignedController(MinecraftServer server, String teamId, List<String> assignedStationIds) {
        if (server == null || teamId == null || teamId.isBlank() || assignedStationIds == null) {
            return null;
        }
        for (String stationId : assignedStationIds) {
            if (stationId == null || stationId.isBlank()) {
                continue;
            }
            ResearchControllerBlockEntity controller = ResearchMultiblockStationRegistry.controllerByStationId(server, teamId, stationId);
            if (controller != null && controller.isFormed()) {
                return controller;
            }
        }
        return null;
    }

    private static JsonObject toJsonPos(BlockPos pos) {
        JsonObject row = new JsonObject();
        if (pos == null) {
            row.addProperty("x", 0);
            row.addProperty("y", 0);
            row.addProperty("z", 0);
            return row;
        }
        row.addProperty("x", pos.getX());
        row.addProperty("y", pos.getY());
        row.addProperty("z", pos.getZ());
        return row;
    }

    private record RunModifierState(
            int powerMultiplierBps,
            int bonusRunChanceBps,
            int corruptionMultiplierBps,
            double speedMultiplier
    ) {
        private static final RunModifierState DEFAULT = new RunModifierState(10_000, 0, 10_000, 1.0D);

        private RunModifierState(int powerMultiplierBps, int bonusRunChanceBps, int corruptionMultiplierBps) {
            this(powerMultiplierBps, bonusRunChanceBps, corruptionMultiplierBps, 1.0D);
        }

        public double powerMultiplier() {
            return Math.max(0.0D, powerMultiplierBps / 10_000.0D);
        }

        public double bonusRunChance() {
            return Math.max(0.0D, bonusRunChanceBps / 10_000.0D);
        }

        public double corruptionMultiplier() {
            return Math.max(0.0D, corruptionMultiplierBps / 10_000.0D);
        }
    }
}
