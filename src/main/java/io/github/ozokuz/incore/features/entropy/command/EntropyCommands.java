package io.github.ozokuz.incore.features.entropy.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.playerlevel.command.PlayerLevelCommands;
import io.github.ozokuz.incore.features.entropy.EntropyManager;
import io.github.ozokuz.incore.features.entropy.network.EntropyNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class EntropyCommands {
    private EntropyCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("entropy")
                                .then(Commands.literal("get")
                                        .executes(ctx -> sendStats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> sendStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(EntropyCommands::setEntropy))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(EntropyCommands::addEntropy))))
                                .then(Commands.literal("fill")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(EntropyCommands::fillEntropy)))
                                .then(Commands.literal("set_cap_bonus")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(EntropyCommands::setCapBonus))))
                                .then(Commands.literal("add_cap_bonus")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(EntropyCommands::addCapBonus)))))
                        .then(PlayerLevelCommands.build())
        );
    }

    private static int sendStats(CommandSourceStack source, ServerPlayer player) {
        int current = EntropyManager.getCurrentEntropy(player);
        int cap = EntropyManager.getEntropyCap(player);
        int bonus = EntropyManager.getEntropyCapBonus(player);
        long intervalSeconds = EntropyManager.getRegenIntervalMillis() / 1000L;
        long next = EntropyManager.getMillisUntilNextIncrease(player);
        long full = EntropyManager.getMillisUntilFull(player);

        source.sendSuccess(() -> Component.literal(String.format(
                "%s entropy: %d/%d (cap bonus: %d, regen every: %ss, next increase: %s, full in: %s)",
                player.getGameProfile().getName(),
                current,
                cap,
                bonus,
                intervalSeconds,
                formatDuration(next),
                formatDuration(full)
        )), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setEntropy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            EntropyManager.setCurrentEntropy(target, value);
            EntropyNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Set entropy to " + value + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int addEntropy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            EntropyManager.addEntropy(target, amount);
            EntropyNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Added " + amount + " entropy for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int fillEntropy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            EntropyManager.setCurrentEntropy(target, EntropyManager.getEntropyCap(target));
            EntropyNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Filled entropy for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int setCapBonus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            EntropyManager.setEntropyCapBonus(target, value);
            EntropyNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Set entropy cap bonus to " + value + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int addCapBonus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            EntropyManager.adjustEntropyCapBonus(target, amount);
            EntropyNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Adjusted entropy cap bonus by " + amount + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static String formatDuration(long millis) {
        if (millis < 0L) {
            return "paused";
        }

        if (millis == 0L) {
            return "full";
        }

        long totalSeconds = Math.max(1L, (millis + 999L) / 1000L);
        long seconds = totalSeconds % 60L;
        long minutes = (totalSeconds / 60L) % 60L;
        long hours = totalSeconds / 3600L;

        if (hours > 0L) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        }

        return String.format("%02d:%02d", minutes, seconds);
    }

}
