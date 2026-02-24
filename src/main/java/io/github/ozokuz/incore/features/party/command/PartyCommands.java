package io.github.ozokuz.incore.features.party.command;

import com.mojang.brigadier.Command;
import io.github.ozokuz.incore.features.party.PartyService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class PartyCommands {
    private PartyCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("party")
                        .requires(source -> source.getEntity() instanceof ServerPlayer)
                        .then(Commands.literal("create")
                                .executes(context -> runForPlayer(context.getSource(), PartyService::createParty)))
                        .then(Commands.literal("invite")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(PartyCommands::invitePlayer)))
                        .then(Commands.literal("accept")
                                .executes(context -> runForPlayer(context.getSource(), PartyService::acceptInvite)))
                        .then(Commands.literal("decline")
                                .executes(context -> runForPlayer(context.getSource(), PartyService::declineInvite)))
                        .then(Commands.literal("leave")
                                .executes(context -> runForPlayer(context.getSource(), PartyService::leaveParty)))
                        .then(Commands.literal("kick")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(PartyCommands::kickPlayer)))
                        .then(Commands.literal("promote")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(PartyCommands::promotePlayer)))
                        .then(Commands.literal("info")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    return PartyService.sendInfo(player);
                                }))
        );
    }

    private static int invitePlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        return PartyService.invite(source, target) ? Command.SINGLE_SUCCESS : 0;
    }

    private static int kickPlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        return PartyService.kickMember(source, target) ? Command.SINGLE_SUCCESS : 0;
    }

    private static int promotePlayer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer source = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        return PartyService.promoteLeader(source, target) ? Command.SINGLE_SUCCESS : 0;
    }

    private static int runForPlayer(CommandSourceStack source, PlayerAction action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        return action.run(player) ? Command.SINGLE_SUCCESS : 0;
    }

    @FunctionalInterface
    private interface PlayerAction {
        boolean run(ServerPlayer player);
    }
}
