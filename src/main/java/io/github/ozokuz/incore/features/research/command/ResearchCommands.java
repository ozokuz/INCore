package io.github.ozokuz.incore.features.research.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.research.ManualResearchTaskManager;
import io.github.ozokuz.incore.features.research.ResearchEntryManager;
import io.github.ozokuz.incore.features.research.ResearchProgressService;
import io.github.ozokuz.incore.features.research.network.ResearchNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.List;

public final class ResearchCommands {
    private ResearchCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("research")
                                .then(Commands.literal("get")
                                        .executes(ctx -> sendStats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> sendStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("open")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ResearchCommands::openResearchScreen)))
                                .then(Commands.literal("enqueue")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                        .executes(ResearchCommands::enqueueResearch))))
                                .then(Commands.literal("force_unlock")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                        .executes(ResearchCommands::forceUnlockResearch))))
                                .then(Commands.literal("revoke")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("entry", ResourceLocationArgument.id())
                                                        .executes(ResearchCommands::revokeResearch))))
                                .then(Commands.literal("complete_task")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("task", ResourceLocationArgument.id())
                                                        .executes(ResearchCommands::completeTask))))
                                .then(Commands.literal("clear_queue")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ResearchCommands::clearQueue)))
                                .then(Commands.literal("reset_all")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ResearchCommands::resetAll))))
        );
    }

    private static int sendStats(CommandSourceStack source, ServerPlayer player) {
        int activeProgress = ResearchProgressService.activeProgress(player);
        SetSummary summary = summarize(player);

        source.sendSuccess(() -> Component.literal(String.format(
                "%s research: activeProgress=%d, queue=%d, unlocked=%d, completedTasks=%d, active=%s",
                player.getGameProfile().getName(),
                activeProgress,
                summary.queueSize,
                summary.unlockedSize,
                summary.completedTaskSize,
                summary.activeEntry
        )), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int openResearchScreen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        ResearchNetworking.openFor(target);
        context.getSource().sendSuccess(
                () -> Component.literal("Opened research screen for " + target.getGameProfile().getName() + "."),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int enqueueResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation entryId = ResourceLocationArgument.getId(context, "entry");

        if (!ResearchEntryManager.all().containsKey(entryId)) {
            context.getSource().sendFailure(Component.literal("Unknown research entry id: " + entryId));
            return 0;
        }

        int queued = 0;
        for (ServerPlayer target : targets) {
            if (ResearchProgressService.enqueueResearch(target, entryId)) {
                queued++;
            }
        }
        int queuedCount = queued;
        int totalTargets = targets.size();

        context.getSource().sendSuccess(
                () -> Component.literal("Queued research " + entryId + " for " + queuedCount + "/" + totalTargets + " player(s)."),
                true
        );

        return queuedCount;
    }

    private static int forceUnlockResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation entryId = ResourceLocationArgument.getId(context, "entry");

        if (!ResearchEntryManager.all().containsKey(entryId)) {
            context.getSource().sendFailure(Component.literal("Unknown research entry id: " + entryId));
            return 0;
        }

        int unlocked = 0;
        for (ServerPlayer target : targets) {
            if (ResearchProgressService.forceUnlockResearch(target, entryId)) {
                unlocked++;
            }
        }
        int unlockedCount = unlocked;
        int totalTargets = targets.size();

        context.getSource().sendSuccess(
                () -> Component.literal("Force-unlocked " + entryId + " for " + unlockedCount + "/" + totalTargets + " player(s)."),
                true
        );

        return unlockedCount;
    }

    private static int completeTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation taskId = ResourceLocationArgument.getId(context, "task");

        if (!ManualResearchTaskManager.all().containsKey(taskId)) {
            context.getSource().sendFailure(Component.literal("Unknown manual research task id: " + taskId));
            return 0;
        }

        int completed = 0;
        for (ServerPlayer target : targets) {
            if (ResearchProgressService.completeTask(target, taskId)) {
                completed++;
            }
        }
        int completedCount = completed;
        int totalTargets = targets.size();

        context.getSource().sendSuccess(
                () -> Component.literal("Marked task " + taskId + " as completed for " + completedCount + "/" + totalTargets + " player(s)."),
                true
        );

        return completedCount;
    }

    private static int revokeResearch(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation entryId = ResourceLocationArgument.getId(context, "entry");

        if (!ResearchEntryManager.all().containsKey(entryId)) {
            context.getSource().sendFailure(Component.literal("Unknown research entry id: " + entryId));
            return 0;
        }

        int revoked = 0;
        for (ServerPlayer target : targets) {
            if (ResearchProgressService.revokeResearch(target, entryId)) {
                revoked++;
            }
        }
        int revokedCount = revoked;
        int totalTargets = targets.size();

        context.getSource().sendSuccess(
                () -> Component.literal("Revoked " + entryId + " for " + revokedCount + "/" + totalTargets + " player(s)."),
                true
        );

        return revokedCount;
    }

    private static int clearQueue(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            ResearchProgressService.clearQueue(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Cleared research queue for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        int reset = 0;
        for (ServerPlayer target : targets) {
            if (ResearchProgressService.resetAllResearch(target)) {
                reset++;
            }
        }
        int resetCount = reset;
        int totalTargets = targets.size();

        context.getSource().sendSuccess(
                () -> Component.literal("Reset all research data for " + resetCount + "/" + totalTargets + " player(s)."),
                true
        );

        return resetCount;
    }

    private static SetSummary summarize(ServerPlayer player) {
        List<ResourceLocation> queue = ResearchProgressService.queuedResearch(player);
        String active = queue.isEmpty() ? "none" : queue.get(0).toString();
        return new SetSummary(
                queue.size(),
                ResearchProgressService.unlocked(player).size(),
                ResearchProgressService.completedTasks(player).size(),
                active
        );
    }

    private record SetSummary(int queueSize, int unlockedSize, int completedTaskSize, String activeEntry) {
    }
}
