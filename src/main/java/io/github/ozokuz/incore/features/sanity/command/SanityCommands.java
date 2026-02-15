package io.github.ozokuz.incore.features.sanity.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.sanity.SanityManager;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class SanityCommands {
    private SanityCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("sanity")
                                .then(Commands.literal("get")
                                        .executes(ctx -> sendStats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(ctx -> sendStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(SanityCommands::setSanity))))
                                .then(Commands.literal("add")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(SanityCommands::addSanity))))
                                .then(Commands.literal("fill")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(SanityCommands::fillSanity)))
                                .then(Commands.literal("set_cap_bonus")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(SanityCommands::setCapBonus))))
                                .then(Commands.literal("add_cap_bonus")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                                        .executes(SanityCommands::addCapBonus)))))
        );
    }

    private static int sendStats(CommandSourceStack source, ServerPlayer player) {
        int current = SanityManager.getCurrentSanity(player);
        int cap = SanityManager.getSanityCap(player);
        int bonus = SanityManager.getSanityCapBonus(player);
        long intervalSeconds = SanityManager.getRegenIntervalMillis() / 1000L;
        long next = SanityManager.getMillisUntilNextIncrease(player);
        long full = SanityManager.getMillisUntilFull(player);

        source.sendSuccess(() -> Component.literal(String.format(
                "%s sanity: %d/%d (cap bonus: %d, regen every: %ss, next increase: %s, full in: %s)",
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

    private static int setSanity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            SanityManager.setCurrentSanity(target, value);
            SanityNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Set sanity to " + value + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int addSanity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            SanityManager.addSanity(target, amount);
            SanityNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Added " + amount + " sanity for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int fillSanity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            SanityManager.setCurrentSanity(target, SanityManager.getSanityCap(target));
            SanityNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Filled sanity for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int setCapBonus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            SanityManager.setSanityCapBonus(target, value);
            SanityNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Set sanity cap bonus to " + value + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int addCapBonus(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            SanityManager.adjustSanityCapBonus(target, amount);
            SanityNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Adjusted sanity cap bonus by " + amount + " for " + targets.size() + " player(s)."),
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
