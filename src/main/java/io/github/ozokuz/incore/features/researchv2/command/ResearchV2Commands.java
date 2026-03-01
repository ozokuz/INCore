package io.github.ozokuz.incore.features.researchv2.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.researchv2.ResearchManager;
import io.github.ozokuz.incore.features.researchv2.registry.ResearchRegistry;
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
                                                .executes(ResearchV2Commands::clearResearch))))
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

    private static int discoverNode(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation nodeId = ResourceLocationArgument.getId(context, "node");
        MinecraftServer server = context.getSource().getServer();

        if (!ResearchRegistry.nodes().containsKey(nodeId)) {
            context.getSource().sendFailure(Component.literal("Unknown research node id: " + nodeId));
            return 0;
        }

        int changed = 0;
        for (ServerPlayer target : targets) {
            String teamId = ResearchTeamResolver.resolveTeamId(target);
            if (teamId == null || teamId.isBlank()) {
                continue;
            }
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
        for (ServerPlayer target : targets) {
            String teamId = ResearchTeamResolver.resolveTeamId(target);
            if (teamId == null || teamId.isBlank()) {
                continue;
            }
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
        for (ServerPlayer target : targets) {
            String teamId = ResearchTeamResolver.resolveTeamId(target);
            if (teamId == null || teamId.isBlank()) {
                continue;
            }
            if (ResearchManager.clearResearch(server, teamId)) {
                changed++;
            }
        }

        int changedCount = changed;
        context.getSource().sendSuccess(() -> Component.literal("Cleared research for " + changedCount + " target team(s)."), true);
        return changedCount;
    }
}
