package io.github.ozokuz.incore.features.research.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.research.ResearchManager;
import io.github.ozokuz.incore.features.research.model.ResearchNodeDefinition;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import io.github.ozokuz.incore.features.research.provider.ResearchProviderManager;
import io.github.ozokuz.incore.features.research.registry.ResearchRegistry;
import io.github.ozokuz.incore.features.research.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.research.state.ResearchQueueEntry;
import io.github.ozokuz.incore.features.research.station.ResearchMultiblockStationRegistry;
import io.github.ozokuz.incore.features.research.station.ResearchStationDescriptor;
import io.github.ozokuz.incore.features.research.team.ResearchTeamResolver;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResearchCommands {
    private ResearchCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("research")
                                .then(Commands.literal("list")
                                        .then(Commands.literal("trees").executes(ResearchCommands::listTrees))
                                        .then(Commands.literal("nodes").executes(ResearchCommands::listNodes)))
                                .then(Commands.literal("get")
                                        .executes(ctx -> getSnapshot(ctx, ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> getSnapshot(ctx, EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("stations")
                                        .executes(ctx -> listStations(ctx, ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> listStations(ctx, EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("queue")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                                        .executes(ResearchCommands::queueNode))))
                                .then(Commands.literal("discover")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                                        .executes(ResearchCommands::discoverNode))))
                                .then(Commands.literal("complete")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                                        .executes(ResearchCommands::completeNode))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ResearchCommands::clearResearch)))
                                .then(Commands.literal("dev_provider")
                                        .then(Commands.literal("setPower")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ResearchCommands::setPower)))
                                        .then(Commands.literal("setMaterials")
                                                .then(Commands.argument("materialId", ResourceLocationArgument.id())
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                                .executes(ResearchCommands::setMaterials))))
                                        .then(Commands.literal("setModules")
                                                .then(Commands.argument("tier", StringArgumentType.word())
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                                .executes(ResearchCommands::setModules))))
                                        .then(Commands.literal("setControllerTier")
                                                .then(Commands.argument("tier", IntegerArgumentType.integer(0))
                                                        .executes(ResearchCommands::setControllerTier))))
                        )
        );
    }

    private static int listTrees(CommandContext<CommandSourceStack> context) {
        String joined = ResearchRegistry.trees().keySet().stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.literal("Research trees (" + ResearchRegistry.trees().size() + "): " + joined), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int listNodes(CommandContext<CommandSourceStack> context) {
        String joined = ResearchRegistry.nodes().keySet().stream().map(ResourceLocation::toString).sorted().collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.literal("Research nodes (" + ResearchRegistry.nodes().size() + "): " + joined), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int getSnapshot(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String teamId = ResearchTeamResolver.resolveTeamId(target);
        MinecraftServer server = context.getSource().getServer();
        var state = ResearchManager.ensureTeamState(server, teamId);
        int effectiveTier = ResearchManager.effectiveControllerTier(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal(
                "Research team=" + teamId
                        + ", controllerTier=" + effectiveTier
                        + ", discovered=" + state.discoveredNodes().size()
                        + ", completed=" + state.completedNodes().size()
                        + ", queue=" + state.researchQueue().size()
        ), false);

        context.getSource().sendSuccess(() -> Component.literal(
                "PowerAvailable=" + ResearchProviderManager.availablePower(server, teamId)
                        + ", materials={" + formatIntMap(state.devResearchMaterials()) + "}"
                        + ", modules={" + formatIntMap(state.devLogicModules()) + "}"
        ), false);

        if (state.researchQueue().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Queue: empty"), false);
            return Command.SINGLE_SUCCESS;
        }

        for (int i = 0; i < state.researchQueue().size(); i++) {
            ResearchQueueEntry entry = state.researchQueue().get(i);
            ResearchNodeDefinition node = ResearchRegistry.nodes().get(entry.nodeId());
            int runTickRequired = entry.runTickRequired() > 0 ? entry.runTickRequired() : (node == null ? 1 : Math.max(1, node.researchTime()));
            int runTickProgress = Math.max(0, Math.min(entry.runTickProgress(), runTickRequired));
            int requiredRuns = entry.requiredRuns() > 0 ? entry.requiredRuns() : (node == null ? 1 : Math.max(1, node.requiredRuns()));
            int completedRuns = Math.max(0, Math.min(entry.completedRuns(), requiredRuns));
            int totalRequired = runTickRequired * requiredRuns;
            int totalProgress = Math.max(0, Math.min(totalRequired, (completedRuns * runTickRequired) + runTickProgress));
            double percent = totalRequired <= 0 ? 0.0D : (100.0D * totalProgress / totalRequired);
            int powerPerTick = estimatePowerPerTick(node, runTickProgress, runTickRequired);

            final int index = i + 1;
            final String line = String.format(
                    "#%d %s status=%s run=%d/%d tick=%d/%d total=%d/%d (%.1f%%), rp/t=%d",
                    index,
                    entry.nodeId(),
                    entry.status(),
                    Math.min(requiredRuns, completedRuns + 1),
                    requiredRuns,
                    runTickProgress,
                    runTickRequired,
                    totalProgress,
                    totalRequired,
                    percent,
                    powerPerTick
            );
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listStations(CommandContext<CommandSourceStack> context, ServerPlayer target) {
        String teamId = ResearchTeamResolver.resolveTeamId(target);
        MinecraftServer server = context.getSource().getServer();
        int effectiveTier = ResearchManager.effectiveControllerTier(server, teamId);
        List<ResearchStationDescriptor> stations = ResearchMultiblockStationRegistry.stationsForTeam(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal(
                "Research stations team=" + teamId
                        + ", effectiveTier=" + effectiveTier
                        + ", formedStations=" + stations.size()
        ), false);

        if (stations.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("Stations: none formed."), false);
            return Command.SINGLE_SUCCESS;
        }

        for (int i = 0; i < stations.size(); i++) {
            ResearchStationDescriptor station = stations.get(i);
            final int index = i + 1;
            final String line = String.format(
                    "#%d id=%s formed=%s tier=%d rpAvailable=%d parts=%d powerFamily=%s inputTier=%d inputs=%d output=%s disk=t%d/%d snap corrupt=%d seg augment[speed=%.2f,power=%.2f,bonus=%.2f,corruption=%.2f]",
                    index,
                    station.stationId(),
                    station.formed(),
                    station.stationTier(),
                    station.availableResearchPower(),
                    station.connectedParts().size(),
                    station.powerFamily(),
                    station.powerInputTier(),
                    station.endpoints().inputs().size(),
                    station.outputPortModes(),
                    station.mountedDiskTier(),
                    station.mountedDiskSnapshotCount(),
                    station.mountedDiskCorruptedSegmentCount(),
                    station.activeSpeedMultiplier(),
                    station.activePowerMultiplier(),
                    station.activeBonusRunChance(),
                    station.activeCorruptionMultiplier()
            );
            context.getSource().sendSuccess(() -> Component.literal(line), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int queueNode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation nodeId = ResourceLocationArgument.getId(context, "node");
        MinecraftServer server = context.getSource().getServer();

        if (!ResearchRegistry.nodes().containsKey(nodeId)) {
            context.getSource().sendFailure(Component.literal("Unknown research node id: " + nodeId));
            return 0;
        }

        Set<String> teamIds = resolveTargetTeamIds(targets);
        int changed = 0;
        String firstFailure = null;
        for (String teamId : teamIds) {
            if (ResearchManager.queueResearch(server, teamId, nodeId)) {
                changed++;
            } else if (firstFailure == null) {
                firstFailure = ResearchManager.explainQueueFailure(server, teamId, nodeId);
            }
        }

        int changedCount = changed;
        int totalTeams = teamIds.size();
        if (changedCount <= 0) {
            String reason = firstFailure == null ? "no eligible team target found" : firstFailure;
            context.getSource().sendFailure(Component.literal("Failed to queue node " + nodeId + ": " + reason + "."));
            return 0;
        }

        String suffix = firstFailure == null ? "" : " First failure: " + firstFailure + ".";
        context.getSource().sendSuccess(() -> Component.literal("Queued node " + nodeId + " for " + changedCount + "/" + totalTeams + " target team(s)." + suffix), true);
        return changedCount;
    }

    private static int discoverNode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation nodeId = ResourceLocationArgument.getId(context, "node");
        MinecraftServer server = context.getSource().getServer();

        if (!ResearchRegistry.nodes().containsKey(nodeId)) {
            context.getSource().sendFailure(Component.literal("Unknown research node id: " + nodeId));
            return 0;
        }

        int changed = 0;
        for (String teamId : resolveTargetTeamIds(targets)) {
            if (ResearchManager.grantDiscovery(server, teamId, nodeId, "command")) {
                changed++;
            }
        }

        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.literal("Discovered node " + nodeId + " for " + changedCount + " target team(s)."), true);
        return changedCount;
    }

    private static int completeNode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation nodeId = ResourceLocationArgument.getId(context, "node");
        MinecraftServer server = context.getSource().getServer();

        if (!ResearchRegistry.nodes().containsKey(nodeId)) {
            context.getSource().sendFailure(Component.literal("Unknown research node id: " + nodeId));
            return 0;
        }

        int changed = 0;
        for (String teamId : resolveTargetTeamIds(targets)) {
            if (ResearchManager.grantCompletion(server, teamId, nodeId)) {
                changed++;
            }
        }

        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.literal("Completed node " + nodeId + " for " + changedCount + " target team(s)."), true);
        return changedCount;
    }

    private static int clearResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        MinecraftServer server = context.getSource().getServer();

        int changed = 0;
        for (String teamId : resolveTargetTeamIds(targets)) {
            if (ResearchManager.clearResearch(server, teamId)) {
                changed++;
            }
        }

        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.literal("Cleared research for " + changedCount + " target team(s)."), true);
        return changedCount;
    }

    private static int setPower(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        String teamId = ResearchTeamResolver.resolveTeamId(sourcePlayer);
        int amount = IntegerArgumentType.getInteger(context, "amount");
        MinecraftServer server = context.getSource().getServer();
        ResearchProviderManager.devProvider().setPower(server, teamId, amount);
        ResearchNetworkSavedData.get(server).setDirty();
        ResearchNetworking.syncTeam(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal("Set team research power to " + amount + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setMaterials(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        String teamId = ResearchTeamResolver.resolveTeamId(sourcePlayer);
        String materialId = ResourceLocationArgument.getId(context, "materialId").toString();
        int count = IntegerArgumentType.getInteger(context, "count");
        MinecraftServer server = context.getSource().getServer();
        ResearchProviderManager.devProvider().setMaterial(server, teamId, materialId, count);
        ResearchNetworkSavedData.get(server).setDirty();
        ResearchNetworking.syncTeam(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal("Set team material '" + materialId + "' to " + count + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setModules(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        String teamId = ResearchTeamResolver.resolveTeamId(sourcePlayer);
        String tier = StringArgumentType.getString(context, "tier");
        int count = IntegerArgumentType.getInteger(context, "count");
        MinecraftServer server = context.getSource().getServer();
        ResearchProviderManager.devProvider().setModule(server, teamId, tier, count);
        ResearchNetworkSavedData.get(server).setDirty();
        ResearchNetworking.syncTeam(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal("Set team module tier '" + tier + "' to " + count + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setControllerTier(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        String teamId = ResearchTeamResolver.resolveTeamId(sourcePlayer);
        int tier = IntegerArgumentType.getInteger(context, "tier");
        MinecraftServer server = context.getSource().getServer();
        boolean changed = ResearchManager.setControllerTier(server, teamId, tier);
        if (!changed) {
            context.getSource().sendSuccess(() -> Component.literal("Controller tier is already set to " + tier + "."), false);
            return Command.SINGLE_SUCCESS;
        }

        context.getSource().sendSuccess(() -> Component.literal("Set team controller tier to " + tier + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static Set<String> resolveTargetTeamIds(Collection<ServerPlayer> targets) {
        Set<String> teamIds = new HashSet<>();
        for (ServerPlayer target : targets) {
            String teamId = ResearchTeamResolver.resolveTeamId(target);
            if (teamId != null && !teamId.isBlank()) {
                teamIds.add(teamId);
            }
        }
        return teamIds;
    }

    private static String formatIntMap(Map<String, Integer> values) {
        if (values.isEmpty()) {
            return "";
        }
        return values.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    private static int estimatePowerPerTick(ResearchNodeDefinition node, int progress, int requiredTime) {
        if (node == null) {
            return 0;
        }
        var power = node.researchPower();
        if (power == null) {
            return 0;
        }
        double ratio = requiredTime <= 0 ? 0.0D : Math.max(0.0D, (double) progress / (double) requiredTime);
        double value = power.baseRpPerTick() + (power.curveScaleRpPerTick() * Math.pow(ratio, power.curveExponent()));
        return Math.max(0, (int) Math.ceil(value));
    }
}
