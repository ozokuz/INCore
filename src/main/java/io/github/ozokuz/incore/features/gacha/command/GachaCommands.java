package io.github.ozokuz.incore.features.gacha.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.gacha.GachaBannerManager;
import io.github.ozokuz.incore.features.gacha.GachaService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class GachaCommands {
    private GachaCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("gacha")
                                .then(Commands.literal("open")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .executes(GachaCommands::openGachaScreen)))
                                .then(Commands.literal("buy_banner")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("banner", ResourceLocationArgument.id())
                                                        .executes(GachaCommands::buyBanner))))
                                .then(Commands.literal("give_basic_permit")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(GachaCommands::giveBasicPermit))))
                                .then(Commands.literal("give_chartered_permit")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(GachaCommands::giveCharteredPermit))))
                                .then(Commands.literal("give_banner_permit")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("banner", ResourceLocationArgument.id())
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(GachaCommands::giveBannerPermit))))))
        );
    }

    private static int openGachaScreen(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        GachaService.openBannerScreen(target);
        context.getSource().sendSuccess(
                () -> Component.literal("Opened gacha banner screen for " + target.getGameProfile().getName() + "."),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int buyBanner(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation bannerId = ResourceLocationArgument.getId(context, "banner");
        if (bannerId == null || GachaBannerManager.get(bannerId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown banner id."));
            return 0;
        }

        for (ServerPlayer target : targets) {
            GachaService.acquireCrateForBanner(target, bannerId);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Attempted gacha crate purchase for " + bannerId + " on " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static int giveBasicPermit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int count = IntegerArgumentType.getInteger(context, "count");
        for (ServerPlayer target : targets) {
            giveOrDrop(target, new ItemStack(Registration.BASIC_BANNER_PERMIT_ITEM.get(), count));
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Gave basic permits x" + count + " to " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static int giveCharteredPermit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int count = IntegerArgumentType.getInteger(context, "count");
        for (ServerPlayer target : targets) {
            giveOrDrop(target, new ItemStack(Registration.CHARTERED_BANNER_PERMIT_ITEM.get(), count));
        }
        context.getSource().sendSuccess(
                () -> Component.literal("Gave chartered permits x" + count + " to " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static int giveBannerPermit(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation bannerId = ResourceLocationArgument.getId(context, "banner");
        if (bannerId == null || GachaBannerManager.get(bannerId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown banner id."));
            return 0;
        }
        int count = IntegerArgumentType.getInteger(context, "count");
        for (ServerPlayer target : targets) {
            giveOrDrop(target, GachaService.createSpecificPermit(bannerId, count));
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Gave banner permits for " + bannerId + " x" + count + " to " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
