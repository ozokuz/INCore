package io.github.ozokuz.incore.features.market.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.ozokuz.incore.features.market.MarketItemDefinition;
import io.github.ozokuz.incore.features.market.MarketItemManager;
import io.github.ozokuz.incore.features.market.MarketPricingService;
import io.github.ozokuz.incore.features.market.MarketService;
import io.github.ozokuz.incore.features.market.network.MarketNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class MarketCommands {
    private MarketCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("market")
                                .then(Commands.literal("status").executes(MarketCommands::status))
                                .then(Commands.literal("inspect")
                                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                                .executes(MarketCommands::inspect)))
                                .then(Commands.literal("force_rollover").executes(MarketCommands::forceRollover))
                                .then(Commands.literal("set_demand")
                                        .then(Commands.argument("item", StringArgumentType.greedyString())
                                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(-10D, 10D))
                                                        .executes(MarketCommands::setDemand))))
                                .then(Commands.literal("sync")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(MarketCommands::sync))))
        );
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        int count = MarketItemManager.all().size();
        context.getSource().sendSuccess(() -> Component.literal("Market items loaded: " + count), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int inspect(CommandContext<CommandSourceStack> context) {
        ResourceLocation itemId = parseItemId(StringArgumentType.getString(context, "item"));
        if (itemId == null) {
            context.getSource().sendFailure(Component.literal("Invalid item id."));
            return 0;
        }

        MarketItemDefinition definition = MarketItemManager.get(itemId);
        if (definition == null) {
            context.getSource().sendFailure(Component.literal("Item is not market-tradeable: " + itemId));
            return 0;
        }

        int current = MarketPricingService.currentPrice(context.getSource().getServer(), itemId);
        context.getSource().sendSuccess(
                () -> Component.literal("Market " + itemId + " -> base=" + definition.basePriceSpur() + " current=" + current),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int forceRollover(CommandContext<CommandSourceStack> context) {
        MarketPricingService.forceRollover(context.getSource().getServer());
        context.getSource().sendSuccess(() -> Component.literal("Forced market rollover complete."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setDemand(CommandContext<CommandSourceStack> context) {
        ResourceLocation itemId = parseItemId(StringArgumentType.getString(context, "item"));
        if (itemId == null) {
            context.getSource().sendFailure(Component.literal("Invalid item id."));
            return 0;
        }

        double value = DoubleArgumentType.getDouble(context, "value");
        boolean applied = MarketPricingService.setDemand(context.getSource().getServer(), itemId, value);
        if (!applied) {
            context.getSource().sendFailure(Component.literal("Item is not market-tradeable: " + itemId));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal("Set demand for " + itemId + " to " + value), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int sync(CommandContext<CommandSourceStack> context) {
        Collection<ServerPlayer> targets;
        try {
            targets = EntityArgument.getPlayers(context, "targets");
        } catch (CommandSyntaxException exception) {
            context.getSource().sendFailure(Component.literal("Failed to resolve players."));
            return 0;
        }
        int synced = 0;
        for (ServerPlayer target : targets) {
            MarketNetworking.openMarketScreen(target, MarketService.buildScreenData(target.getServer(), false, null));
            synced++;
        }

        int syncedCount = synced;
        context.getSource().sendSuccess(() -> Component.literal("Synced market UI snapshot to " + syncedCount + " player(s)."), true);
        return synced;
    }

    private static ResourceLocation parseItemId(String raw) {
        return ResourceLocation.tryParse(raw.trim());
    }
}
