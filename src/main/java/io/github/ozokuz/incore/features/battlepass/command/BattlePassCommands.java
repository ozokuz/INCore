package io.github.ozokuz.incore.features.battlepass.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.ozokuz.incore.features.battlepass.BattlePassDefinition;
import io.github.ozokuz.incore.features.battlepass.BattlePassLane;
import io.github.ozokuz.incore.features.battlepass.BattlePassLaneManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassProgressManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassWeekTime;
import io.github.ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import io.github.ozokuz.incore.features.entropy.network.EntropyNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public final class BattlePassCommands {
    private BattlePassCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("battlepass")
                                .then(Commands.literal("status")
                                        .executes(ctx -> status(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> status(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("set_id", ResourceLocationArgument.id())
                                                .suggests(BattlePassCommands::suggestSetIds)
                                                .executes(BattlePassCommands::setBattlePass)))
                                .then(Commands.literal("next")
                                        .executes(ctx -> rotateBattlePass(ctx.getSource(), 1)))
                                .then(Commands.literal("previous")
                                        .executes(ctx -> rotateBattlePass(ctx.getSource(), -1)))
                                .then(Commands.literal("week")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("week", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> setWeek(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "week")))))
                                        .then(Commands.literal("next")
                                                .executes(ctx -> rotateWeek(ctx.getSource(), 1)))
                                        .then(Commands.literal("previous")
                                                .executes(ctx -> rotateWeek(ctx.getSource(), -1))))
                                .then(Commands.literal("xp")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> setXp(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                                .executes(ctx -> addXp(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))
                                .then(levelCommandLiteral("level"))
                                .then(levelCommandLiteral("tier"))
                                .then(Commands.literal("lane")
                                        .then(Commands.literal("unlock")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("lane", StringArgumentType.word())
                                                                .suggests(BattlePassCommands::suggestLaneIds)
                                                                .executes(BattlePassCommands::unlockLane))))
                                        .then(Commands.literal("lock")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("lane", StringArgumentType.word())
                                                                .suggests(BattlePassCommands::suggestLaneIds)
                                                                .executes(BattlePassCommands::lockLane))))
                                        .then(Commands.literal("list")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .executes(BattlePassCommands::listLanes))))
                                .then(Commands.literal("reset")
                                        .then(Commands.literal("task")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("task_id", StringArgumentType.word())
                                                                .suggests(BattlePassCommands::suggestActiveTaskIds)
                                                                .executes(BattlePassCommands::resetSingleTask))))
                                        .then(Commands.literal("tasks")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(BattlePassCommands::resetAllTasks)))
                                        .then(Commands.literal("all")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .executes(BattlePassCommands::resetAllProgress))))
                                .then(Commands.literal("complete_task")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("task_id", StringArgumentType.word())
                                                        .suggests(BattlePassCommands::suggestActiveTaskIds)
                                                        .executes(BattlePassCommands::completeTask))))
                                .then(Commands.literal("progress_task")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("task_id", StringArgumentType.word())
                                                        .suggests(BattlePassCommands::suggestActiveTaskIds)
                                                        .executes(ctx -> progressTask(ctx, 1))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> progressTask(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> levelCommandLiteral(String literalName) {
        return Commands.literal(literalName)
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                        .executes(ctx -> setLevel(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> addLevel(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))));
    }

    private static CompletableFuture<Suggestions> suggestActiveTaskIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return BattlePassManager.getActiveSet(context.getSource().getServer(), now(context.getSource()))
                .map(BattlePassDefinition::tasks)
                .map(tasks -> tasks.stream().map(BattlePassDefinition.BattlePassTask::id).distinct())
                .map(taskIds -> SharedSuggestionProvider.suggest(taskIds, builder))
                .orElseGet(builder::buildFuture);
    }

    private static CompletableFuture<Suggestions> suggestSetIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BattlePassManager.getKnownSetIds(), builder);
    }

    private static CompletableFuture<Suggestions> suggestLaneIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BattlePassLaneManager.getAllLaneIds(), builder);
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        BattlePassProgressManager.StatusResult result = BattlePassProgressManager.getStatus(player, now(source));
        if ("none".equals(result.setId())) {
            source.sendFailure(Component.literal("No active battle pass."));
            return 0;
        }

        source.sendSuccess(() -> Component.literal(String.format(
                "%s battle pass [%s] level %d (%d/%d xp), current week %d",
                player.getGameProfile().getName(),
                result.setId(),
                result.level(),
                result.xp(),
                result.xpPerLevel(),
                result.currentWeek()
        )), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int completeTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String taskId = StringArgumentType.getString(context, "task_id");
        Instant now = now(context.getSource());
        int successCount = 0;

        for (ServerPlayer target : targets) {
            BattlePassProgressManager.CompletionResult result = BattlePassProgressManager.completeTask(target, taskId, now);
            if (result.success()) {
                successCount++;
            }

            String message = target.getGameProfile().getName() + ": " + result.message();
            if (result.success()) {
                message += " +" + result.xpGained() + " xp";
                if (result.levelsGained() > 0) {
                    message += ", +" + result.levelsGained() + " level(s) (now " + result.newLevel() + ")";
                }
                String successMessage = message;
                context.getSource().sendSuccess(() -> Component.literal(successMessage), true);
                EntropyNetworking.syncToPlayer(target);
            } else {
                context.getSource().sendFailure(Component.literal(message));
            }

            BattlePassNetworking.syncToPlayer(target);
        }

        return successCount;
    }

    private static int progressTask(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String taskId = StringArgumentType.getString(context, "task_id");
        Instant now = now(context.getSource());
        int successCount = 0;

        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ProgressResult result = BattlePassProgressManager.addTaskProgress(target, taskId, amount, now);
            if (result.success()) {
                successCount++;
            }

            String message = target.getGameProfile().getName() + ": " + result.message() + " (" + result.currentProgress() + "/" + result.goalProgress() + ")";
            if (result.success() && result.completedNow()) {
                message += " +" + result.xpGained() + " xp";
                if (result.levelsGained() > 0) {
                    message += ", +" + result.levelsGained() + " level(s) (now " + result.newLevel() + ")";
                }
            }

            if (result.success()) {
                String successMessage = message;
                context.getSource().sendSuccess(() -> Component.literal(successMessage), true);
                if (result.completedNow()) {
                    EntropyNetworking.syncToPlayer(target);
                }
            } else {
                context.getSource().sendFailure(Component.literal(message));
            }

            BattlePassNetworking.syncToPlayer(target);
        }

        return successCount;
    }

    private static int setXp(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        Instant now = now(context.getSource());
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.setXp(target, amount, now);
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int addXp(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        Instant now = now(context.getSource());
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.addXp(target, amount, now);
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int setLevel(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        Instant now = now(context.getSource());
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.setLevel(target, amount, now);
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int addLevel(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        Instant now = now(context.getSource());
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.addLevel(target, amount, now);
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int unlockLane(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String lane = StringArgumentType.getString(context, "lane");
        Instant now = now(context.getSource());

        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.LaneManagementResult result = BattlePassProgressManager.unlockLane(target, lane, now);
            String message = target.getGameProfile().getName() + ": " + result.message();
            if (result.success()) {
                successCount++;
                String successMessage = message;
                context.getSource().sendSuccess(() -> Component.literal(successMessage), true);
            } else {
                context.getSource().sendFailure(Component.literal(message));
            }
            BattlePassNetworking.syncToPlayer(target);
        }

        return successCount;
    }

    private static int lockLane(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String lane = StringArgumentType.getString(context, "lane");
        Instant now = now(context.getSource());

        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.LaneManagementResult result = BattlePassProgressManager.lockLane(target, lane, now);
            String message = target.getGameProfile().getName() + ": " + result.message();
            if (result.success()) {
                successCount++;
                String successMessage = message;
                context.getSource().sendSuccess(() -> Component.literal(successMessage), true);
            } else {
                context.getSource().sendFailure(Component.literal(message));
            }
            BattlePassNetworking.syncToPlayer(target);
        }

        return successCount;
    }

    private static int listLanes(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        BattlePassProgressManager.LaneStatusResult result = BattlePassProgressManager.laneStatus(target, now(context.getSource()));
        if (!result.success()) {
            context.getSource().sendFailure(Component.literal(result.message()));
            return 0;
        }

        String laneLine = result.lanes().stream()
                .map(lane -> lane.id() + "=" + (lane.unlocked() ? "unlocked" : "locked"))
                .collect(Collectors.joining(", "));
        context.getSource().sendSuccess(() -> Component.literal(target.getGameProfile().getName() + " lanes (" + result.setId() + "): " + laneLine), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int resetSingleTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String taskId = StringArgumentType.getString(context, "task_id");
        Instant now = now(context.getSource());

        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.resetTask(target, taskId, now);
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int resetAllTasks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        Instant now = now(context.getSource());

        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.resetAllTasks(target, now);
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int resetAllProgress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        Instant now = now(context.getSource());

        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.resetAllProgress(target, now);
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int setBattlePass(CommandContext<CommandSourceStack> context) {
        ResourceLocation setId = ResourceLocationArgument.getId(context, "set_id");

        return BattlePassManager.setForcedSet(context.getSource().getServer(), setId, now(context.getSource()))
                .map(definition -> {
                    syncAllPlayers(context.getSource());
                    context.getSource().sendSuccess(
                            () -> Component.literal("Set active battle pass to " + definition.id() + " starting at current week start."),
                            true
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    context.getSource().sendFailure(Component.literal("Unknown battle pass: " + setId));
                    return 0;
                });
    }

    private static int rotateBattlePass(CommandSourceStack source, int direction) {
        return BattlePassManager.rotateForcedSet(source.getServer(), direction, now(source))
                .map(definition -> {
                    syncAllPlayers(source);
                    String action = direction >= 0 ? "next" : "previous";
                    source.sendSuccess(
                            () -> Component.literal("Rotated to " + action + " battle pass set: " + definition.id() + " (start aligned to current week start)."),
                            true
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    source.sendFailure(Component.literal("No battle pass sets are loaded."));
                    return 0;
                });
    }

    private static int setWeek(CommandSourceStack source, int week) {
        Instant now = now(source);
        return BattlePassManager.getActiveSet(source.getServer(), now)
                .map(definition -> {
                    int totalWeeks = Math.max(1, (int) definition.durationWeeks());
                    if (week < 1 || week > totalWeeks) {
                        source.sendFailure(Component.literal("Week must be between 1 and " + totalWeeks + "."));
                        return 0;
                    }

                    BattlePassManager.setForcedWeek(week);
                    syncAllPlayers(source);
                    source.sendSuccess(
                            () -> Component.literal("Forced active week set to " + week + "/" + totalWeeks + " for " + definition.id()),
                            true
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    source.sendFailure(Component.literal("No active battle pass set."));
                    return 0;
                });
    }

    private static int rotateWeek(CommandSourceStack source, int direction) {
        Instant now = now(source);
        return BattlePassManager.getActiveSet(source.getServer(), now)
                .map(definition -> {
                    int totalWeeks = Math.max(1, (int) definition.durationWeeks());
                    int currentWeek = BattlePassManager.resolveCurrentWeek(definition, now);
                    int delta = direction >= 0 ? 1 : -1;
                    int nextWeek = Math.floorMod((currentWeek - 1) + delta, totalWeeks) + 1;

                    BattlePassManager.setForcedWeek(nextWeek);
                    syncAllPlayers(source);
                    String action = direction >= 0 ? "next" : "previous";
                    int finalNextWeek = nextWeek;
                    source.sendSuccess(
                            () -> Component.literal("Rotated to " + action + " week: " + finalNextWeek + "/" + totalWeeks + " for " + definition.id()),
                            true
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    source.sendFailure(Component.literal("No active battle pass set."));
                    return 0;
                });
    }

    private static Instant now(CommandSourceStack source) {
        return BattlePassWeekTime.now(source.getServer()).toInstant();
    }

    private static void syncAllPlayers(CommandSourceStack source) {
        source.getServer().getPlayerList().getPlayers().forEach(BattlePassNetworking::syncToPlayer);
    }

    private static int reportManagementResult(CommandSourceStack source, ServerPlayer target, BattlePassProgressManager.ManagementResult result) {
        String message = target.getGameProfile().getName() + ": " + result.message();
        if (result.success()) {
            String successMessage = message;
            source.sendSuccess(() -> Component.literal(successMessage), true);
            BattlePassNetworking.syncToPlayer(target);
            return 1;
        }

        source.sendFailure(Component.literal(message));
        BattlePassNetworking.syncToPlayer(target);
        return 0;
    }
}
