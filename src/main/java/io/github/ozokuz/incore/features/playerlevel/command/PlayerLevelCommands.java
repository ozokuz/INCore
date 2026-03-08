package io.github.ozokuz.incore.features.playerlevel.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelManager;
import io.github.ozokuz.incore.features.playerlevel.network.PlayerLevelNetworking;
import io.github.ozokuz.incore.features.entropy.network.EntropyNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

public final class PlayerLevelCommands {
    private PlayerLevelCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("level")
                .then(Commands.literal("get")
                        .executes(ctx -> sendStats(ctx.getSource(), ctx.getSource().getPlayerOrException()))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> sendStats(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(PlayerLevelCommands::setLevel))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(PlayerLevelCommands::addLevel))))
                .then(Commands.literal("set_xp")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(PlayerLevelCommands::setExperience))))
                .then(Commands.literal("add_xp")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(PlayerLevelCommands::addExperience))));
    }

    private static int sendStats(CommandSourceStack source, ServerPlayer player) {
        int level = PlayerLevelManager.getLevel(player);
        int experience = PlayerLevelManager.getCurrentExperience(player);
        int nextLevelCost = PlayerLevelManager.getExperienceToNextLevel(player);

        source.sendSuccess(() -> Component.literal(String.format(
                "%s level: %d (%d/%d xp to next level)",
                player.getGameProfile().getName(),
                level,
                experience,
                nextLevelCost
        )), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            PlayerLevelManager.setLevel(target, value, false);
            PlayerLevelNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Set player level to " + value + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int addLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            PlayerLevelManager.addLevels(target, amount, amount > 0);
            PlayerLevelNetworking.syncToPlayer(target);
            EntropyNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Adjusted player level by " + amount + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int setExperience(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            PlayerLevelManager.setCurrentExperience(target, value);
            PlayerLevelNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Set player level experience to " + value + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }

    private static int addExperience(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            PlayerLevelManager.addExperience(target, amount);
            PlayerLevelNetworking.syncToPlayer(target);
            EntropyNetworking.syncToPlayer(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Adjusted player level experience by " + amount + " for " + targets.size() + " player(s)."),
                true
        );

        return targets.size();
    }
}
