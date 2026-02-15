package io.github.ozokuz.incore.features.tasks.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.github.ozokuz.incore.features.tasks.TaskService;
import io.github.ozokuz.incore.features.tasks.network.TaskNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class TaskCommands {
    private TaskCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("tasks")
                                .then(Commands.literal("status")
                                        .executes(ctx -> sendStatus(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> sendStatus(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("sync")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(TaskCommands::syncTasks)))
                                .then(Commands.literal("daily")
                                        .then(Commands.literal("reset")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(TaskCommands::resetDaily)))
                                        .then(Commands.literal("complete")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(TaskCommands::completeDaily)))
                                        .then(Commands.literal("claim")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(TaskCommands::claimDaily))))
                                .then(Commands.literal("weekly")
                                        .then(Commands.literal("reset")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(TaskCommands::resetWeekly)))
                                        .then(Commands.literal("complete")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(TaskCommands::completeWeekly)))
                                        .then(Commands.literal("complete_slot")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("slot", IntegerArgumentType.integer(1))
                                                                .executes(TaskCommands::completeWeeklySlot))))
                                        .then(Commands.literal("claim")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(TaskCommands::claimWeekly)))
                                        .then(Commands.literal("claim_tier")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("tier", IntegerArgumentType.integer(1, 5))
                                                                .executes(TaskCommands::claimWeeklyTier))))))
        );
    }

    private static int sendStatus(CommandSourceStack source, ServerPlayer player) {
        TaskService.TaskAdminStatus status = TaskService.adminStatus(player);
        source.sendSuccess(() -> Component.literal(String.format(
                "%s tasks -> daily: %d (completed: %s, claimed: %s), weekly: %d (points: %d, tiers claimed: %d, tiers claimable: %d)",
                player.getGameProfile().getName(),
                status.dailyTaskCount(),
                status.dailyCompleted(),
                status.dailyRewardClaimed(),
                status.weeklyTaskCount(),
                status.weeklyPoints(),
                status.weeklyClaimedTiers(),
                status.weeklyClaimableTiers()
        )), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int syncTasks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            TaskService.tick(target);
            TaskNetworking.syncToPlayer(target);
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Synced tasks for " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static int resetDaily(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            TaskService.forceResetDaily(target);
            TaskNetworking.syncToPlayer(target);
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Reset daily tasks for " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static int completeDaily(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int completedTasks = 0;
        for (ServerPlayer target : targets) {
            completedTasks += TaskService.completeActiveDailyTasks(target);
            TaskNetworking.syncToPlayer(target);
        }
        int completedSnapshot = completedTasks;
        context.getSource().sendSuccess(
                () -> Component.literal("Completed " + completedSnapshot + " active daily task(s) across " + targets.size() + " player(s)."),
                true
        );
        return completedTasks;
    }

    private static int claimDaily(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int claimed = 0;
        for (ServerPlayer target : targets) {
            if (TaskService.claimDailyCompletionReward(target)) {
                claimed++;
            }
            TaskNetworking.syncToPlayer(target);
        }
        int claimedSnapshot = claimed;
        context.getSource().sendSuccess(
                () -> Component.literal("Claimed daily completion reward for " + claimedSnapshot + "/" + targets.size() + " player(s)."),
                true
        );
        return claimed;
    }

    private static int resetWeekly(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            TaskService.forceResetWeekly(target);
            TaskNetworking.syncToPlayer(target);
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Reset weekly tasks for " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static int completeWeekly(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int completedTasks = 0;
        for (ServerPlayer target : targets) {
            completedTasks += TaskService.completeActiveWeeklyTasks(target);
            TaskNetworking.syncToPlayer(target);
        }
        int completedSnapshot = completedTasks;
        context.getSource().sendSuccess(
                () -> Component.literal("Completed " + completedSnapshot + " active weekly task(s) across " + targets.size() + " player(s)."),
                true
        );
        return completedTasks;
    }

    private static int claimWeekly(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int claimedTiers = 0;
        for (ServerPlayer target : targets) {
            claimedTiers += TaskService.claimUnlockedWeeklyTierRewards(target);
            TaskNetworking.syncToPlayer(target);
        }
        int claimedTiersSnapshot = claimedTiers;
        context.getSource().sendSuccess(
                () -> Component.literal("Claimed " + claimedTiersSnapshot + " weekly tier reward(s) across " + targets.size() + " player(s)."),
                true
        );
        return claimedTiers;
    }

    private static int completeWeeklySlot(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int slot = IntegerArgumentType.getInteger(context, "slot");
        int completed = 0;

        for (ServerPlayer target : targets) {
            if (TaskService.completeWeeklyTaskAtSlot(target, slot)) {
                completed++;
            }
            TaskNetworking.syncToPlayer(target);
        }

        int completedSnapshot = completed;
        context.getSource().sendSuccess(
                () -> Component.literal("Completed weekly task slot " + slot + " for " + completedSnapshot + "/" + targets.size() + " player(s)."),
                true
        );
        return completed;
    }

    private static int claimWeeklyTier(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int tier = IntegerArgumentType.getInteger(context, "tier");
        int claimed = 0;

        for (ServerPlayer target : targets) {
            if (TaskService.claimWeeklyTierReward(target, tier)) {
                claimed++;
            }
            TaskNetworking.syncToPlayer(target);
        }

        int claimedSnapshot = claimed;
        context.getSource().sendSuccess(
                () -> Component.literal("Claimed weekly tier " + tier + " reward for " + claimedSnapshot + "/" + targets.size() + " player(s)."),
                true
        );
        return claimed;
    }
}
