package io.github.ozokuz.incore.features.researchv2.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.network.ResearchV2Networking;
import io.github.ozokuz.incore.features.researchv2.provider.ResearchProviderManager;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
import io.github.ozokuz.incore.features.researchv2.state.ResearchNetworkSavedData;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class ResearchV2Commands {
    private ResearchV2Commands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("research_v2")
                                .then(Commands.literal("list")
                                        .then(Commands.literal("trees").executes(ResearchV2Commands::listTrees))
                                        .then(Commands.literal("nodes").executes(ResearchV2Commands::listNodes)))
                                .then(Commands.literal("get")
                                        .executes(ctx -> getSnapshot(ctx, ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> getSnapshot(ctx, EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("queue")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                                        .executes(ResearchV2Commands::queueNode))))
                                .then(Commands.literal("discover")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                                        .executes(ResearchV2Commands::discoverNode))))
                                .then(Commands.literal("complete")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("node", ResourceLocationArgument.id())
                                                        .executes(ResearchV2Commands::completeNode))))
                                .then(Commands.literal("clear")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ResearchV2Commands::clearResearch)))
                                .then(Commands.literal("dev_provider")
                                        .then(Commands.literal("setPower")
                                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                        .executes(ResearchV2Commands::setPower)))
                                        .then(Commands.literal("setMaterials")
                                                .then(Commands.argument("materialId", StringArgumentType.word())
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                                .executes(ResearchV2Commands::setMaterials))))
                                        .then(Commands.literal("setModules")
                                                .then(Commands.argument("tier", StringArgumentType.word())
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(0))
                                                                .executes(ResearchV2Commands::setModules)))))
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
        if (teamId == null || teamId.isBlank()) {
            context.getSource().sendFailure(Component.literal("Target has no research team."));
            return 0;
        }

        String json = ResearchManager.snapshotJson(context.getSource().getServer(), teamId);
        context.getSource().sendSuccess(() -> Component.literal(json), false);
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

        int changed = 0;
        for (String teamId : resolveTargetTeamIds(targets)) {
            if (ResearchManager.queueResearch(server, teamId, nodeId)) {
                changed++;
            }
        }

        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.literal("Queued node " + nodeId + " for " + changedCount + " target team(s)."), true);
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
        if (teamId == null || teamId.isBlank()) {
            context.getSource().sendFailure(Component.literal("You are not in a research team."));
            return 0;
        }

        int amount = IntegerArgumentType.getInteger(context, "amount");
        MinecraftServer server = context.getSource().getServer();
        ResearchProviderManager.devProvider().setPower(server, teamId, amount);
        ResearchNetworkSavedData.get(server).setDirty();
        ResearchV2Networking.syncTeam(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal("Set team research power to " + amount + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setMaterials(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        String teamId = ResearchTeamResolver.resolveTeamId(sourcePlayer);
        if (teamId == null || teamId.isBlank()) {
            context.getSource().sendFailure(Component.literal("You are not in a research team."));
            return 0;
        }

        String materialId = StringArgumentType.getString(context, "materialId");
        int count = IntegerArgumentType.getInteger(context, "count");
        MinecraftServer server = context.getSource().getServer();
        ResearchProviderManager.devProvider().setMaterial(server, teamId, materialId, count);
        ResearchNetworkSavedData.get(server).setDirty();
        ResearchV2Networking.syncTeam(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal("Set team material '" + materialId + "' to " + count + "."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setModules(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer sourcePlayer = context.getSource().getPlayerOrException();
        String teamId = ResearchTeamResolver.resolveTeamId(sourcePlayer);
        if (teamId == null || teamId.isBlank()) {
            context.getSource().sendFailure(Component.literal("You are not in a research team."));
            return 0;
        }

        String tier = StringArgumentType.getString(context, "tier");
        int count = IntegerArgumentType.getInteger(context, "count");
        MinecraftServer server = context.getSource().getServer();
        ResearchProviderManager.devProvider().setModule(server, teamId, tier, count);
        ResearchNetworkSavedData.get(server).setDirty();
        ResearchV2Networking.syncTeam(server, teamId);

        context.getSource().sendSuccess(() -> Component.literal("Set team module tier '" + tier + "' to " + count + "."), true);
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
}
