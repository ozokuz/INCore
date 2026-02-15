package io.github.ozokuz.incore.features.battlepass.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.battlepass.BattlePassProgressManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.time.Instant;
import java.util.Collection;

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
                                .then(Commands.literal("complete_task")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("task_id", StringArgumentType.word())
                                                        .executes(BattlePassCommands::completeTask)))))
        );
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
                context.getSource().sendSuccess(() -> Component.literal(message), true);
            } else {
                context.getSource().sendFailure(Component.literal(message));
            }
        }

        return successCount;
    }
}
