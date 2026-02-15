package io.github.ozokuz.incore.features.battlepass.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.ozokuz.incore.features.battlepass.BattlePassDefinition;
import io.github.ozokuz.incore.features.battlepass.BattlePassManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassProgressManager;
import io.github.ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.time.Instant;
import java.util.Collection;
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
                                        .then(Commands.argument("set_id", StringArgumentType.word())
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
                                .then(Commands.literal("tier")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0))
                                                                .executes(ctx -> setTier(ctx, IntegerArgumentType.getInteger(ctx, "amount"))))))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                                .executes(ctx -> addTier(ctx, IntegerArgumentType.getInteger(ctx, "amount")))))))
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

    private static CompletableFuture<Suggestions> suggestActiveTaskIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return BattlePassManager.getActiveSet(Instant.now())
                .map(BattlePassDefinition::tasks)
                .map(tasks -> tasks.stream().map(BattlePassDefinition.BattlePassTask::id).distinct())
                .map(taskIds -> SharedSuggestionProvider.suggest(taskIds, builder))
                .orElseGet(builder::buildFuture);
    }

    private static CompletableFuture<Suggestions> suggestSetIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(BattlePassManager.getKnownSetIds(), builder);
    }

    private static int status(CommandSourceStack source, ServerPlayer player) {
        BattlePassProgressManager.StatusResult result = BattlePassProgressManager.getStatus(player, Instant.now());
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
        int successCount = 0;

        for (ServerPlayer target : targets) {
            BattlePassProgressManager.CompletionResult result = BattlePassProgressManager.completeTask(target, taskId, Instant.now());
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
                SanityNetworking.syncToPlayer(target);
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
        int successCount = 0;

        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ProgressResult result = BattlePassProgressManager.addTaskProgress(target, taskId, amount, Instant.now());
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
                    SanityNetworking.syncToPlayer(target);
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
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.setXp(target, amount, Instant.now());
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int addXp(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.addXp(target, amount, Instant.now());
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int setTier(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.setLevel(target, amount, Instant.now());
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int addTier(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.addLevel(target, amount, Instant.now());
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int resetSingleTask(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        String taskId = StringArgumentType.getString(context, "task_id");
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.resetTask(target, taskId, Instant.now());
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int resetAllTasks(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.resetAllTasks(target, Instant.now());
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int resetAllProgress(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int successCount = 0;
        for (ServerPlayer target : targets) {
            BattlePassProgressManager.ManagementResult result = BattlePassProgressManager.resetAllProgress(target, Instant.now());
            successCount += reportManagementResult(context.getSource(), target, result);
        }
        return successCount;
    }

    private static int setBattlePass(CommandContext<CommandSourceStack> context) {
        String rawSetId = StringArgumentType.getString(context, "set_id");
        ResourceLocation setId = ResourceLocation.tryParse(rawSetId);
        if (setId == null) {
            context.getSource().sendFailure(Component.literal("Invalid battle pass id: " + rawSetId));
            return 0;
        }

        return BattlePassManager.setForcedSet(setId)
                .map(definition -> {
                    syncAllPlayers(context.getSource());
                    context.getSource().sendSuccess(
                            () -> Component.literal("Forced active battle pass set to " + definition.id()),
                            true
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    context.getSource().sendFailure(Component.literal("Unknown battle pass set: " + setId));
                    return 0;
                });
    }

    private static int rotateBattlePass(CommandSourceStack source, int direction) {
        return BattlePassManager.rotateForcedSet(direction, Instant.now())
                .map(definition -> {
                    syncAllPlayers(source);
                    String action = direction >= 0 ? "next" : "previous";
                    source.sendSuccess(
                            () -> Component.literal("Rotated to " + action + " battle pass set: " + definition.id()),
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
        Instant now = Instant.now();
        return BattlePassManager.getActiveSet(now)
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
        Instant now = Instant.now();
        return BattlePassManager.getActiveSet(now)
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
