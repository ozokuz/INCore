package ozokuz.incore.features.cards.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import ozokuz.incore.Registration;
import ozokuz.incore.features.cards.CardBoosterBoxData;
import ozokuz.incore.features.cards.CardBoosterBoxManager;
import ozokuz.incore.features.cards.CardBoosterManager;
import ozokuz.incore.features.cards.CardChaoticService;
import ozokuz.incore.features.cards.CardCollectionService;
import ozokuz.incore.features.cards.CardDeckBoxData;
import ozokuz.incore.features.cards.CardDeckBoxManager;
import ozokuz.incore.features.cards.CardDeckCoreData;
import ozokuz.incore.features.cards.CardDeckCoreManager;
import ozokuz.incore.features.cards.CardDeckService;
import ozokuz.incore.features.cards.CardItemData;
import ozokuz.incore.features.cards.CardItemFactory;
import ozokuz.incore.features.cards.CardModuleData;
import ozokuz.incore.features.cards.CardModuleManager;
import ozokuz.incore.features.cards.CardModuleType;
import ozokuz.incore.features.cards.CardPackService;
import ozokuz.incore.features.cards.CardSetData;
import ozokuz.incore.features.cards.CardSetManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class CardCommands {
    private static final SuggestionProvider<CommandSourceStack> MODULE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    CardModuleManager.all().stream().map(CardModuleData::id),
                    builder
            );
    private static final SuggestionProvider<CommandSourceStack> CORE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    CardDeckCoreManager.all().stream().map(CardDeckCoreData::id),
                    builder
            );
    private static final SuggestionProvider<CommandSourceStack> DECK_BOX_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    CardDeckBoxManager.all().stream().map(CardDeckBoxData::id),
                    builder
            );
    private static final SuggestionProvider<CommandSourceStack> SET_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    CardSetManager.all().stream().map(CardSetData::id),
                    builder
            );
    private static final SuggestionProvider<CommandSourceStack> BOOSTER_BOX_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    CardBoosterBoxManager.all().stream().map(CardBoosterBoxData::id),
                    builder
            );

    private CardCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("cards")
                                .then(Commands.literal("give_module")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("card", ResourceLocationArgument.id()).suggests(MODULE_SUGGESTIONS)
                                                        .executes(CardCommands::giveModule)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveModule)))))
                                .then(Commands.literal("give_booster")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("set", ResourceLocationArgument.id()).suggests(SET_SUGGESTIONS)
                                                        .executes(CardCommands::giveBooster)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveBooster)))))
                                .then(Commands.literal("give_set_booster")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("set", ResourceLocationArgument.id()).suggests(SET_SUGGESTIONS)
                                                        .executes(CardCommands::giveSetBooster)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveSetBooster)))))
                                .then(Commands.literal("give_booster_box")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("box", ResourceLocationArgument.id()).suggests(BOOSTER_BOX_SUGGESTIONS)
                                                        .executes(CardCommands::giveBoosterBox)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveBoosterBox)))))
                                .then(Commands.literal("give_core")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("core", ResourceLocationArgument.id()).suggests(CORE_SUGGESTIONS)
                                                        .executes(CardCommands::giveCore)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveCore)))))
                                .then(Commands.literal("give_deck_box")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("box", ResourceLocationArgument.id()).suggests(DECK_BOX_SUGGESTIONS)
                                                        .executes(CardCommands::giveDeckBox)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveDeckBox)))))
                                .then(Commands.literal("give_sleeve")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(CardCommands::giveSleeve)
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveSleeve))))
                                .then(Commands.literal("give_tokens")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(CardCommands::giveTokens)
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::giveTokens))))
                                .then(Commands.literal("open_booster")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("set", ResourceLocationArgument.id()).suggests(SET_SUGGESTIONS)
                                                        .executes(CardCommands::openBooster)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1)).executes(CardCommands::openBooster)))))
                                .then(Commands.literal("deck_assemble")
                                        .then(Commands.argument("targets", EntityArgument.players()).executes(CardCommands::assembleDeck)))
                                .then(Commands.literal("set_integrity")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("value", IntegerArgumentType.integer(0)).executes(CardCommands::setIntegrity))))
                                .then(Commands.literal("brick")
                                        .then(Commands.argument("targets", EntityArgument.players()).executes(context -> setBricked(context, true))))
                                .then(Commands.literal("unbrick_debug")
                                        .then(Commands.argument("targets", EntityArgument.players()).executes(context -> setBricked(context, false))))
                                .then(Commands.literal("collection")
                                        .then(Commands.argument("target", EntityArgument.player()).executes(CardCommands::collection)))
                                .then(Commands.literal("debug_deck")
                                        .then(Commands.argument("target", EntityArgument.player()).executes(CardCommands::debugDeck)))
                        )
        );
    }

    private static int getOptionalCount(CommandContext<CommandSourceStack> context) {
        try {
            return IntegerArgumentType.getInteger(context, "count");
        } catch (IllegalArgumentException ignored) {
            return 1;
        }
    }

    private static int giveModule(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation cardId = ResourceLocationArgument.getId(context, "card");
        int count = getOptionalCount(context);

        CardModuleData module = CardModuleManager.get(cardId);
        if (module == null) {
            context.getSource().sendFailure(Component.literal("Unknown card module id: " + cardId));
            return 0;
        }

        for (ServerPlayer target : targets) {
            boolean revealed = module.moduleType() != CardModuleType.CRYPTIC;
            CardChaoticService.ChaoticRoll chaoticRoll = CardChaoticService.roll(module, target.getRandom());
            CardItemData.CardInstance instance = new CardItemData.CardInstance(
                    cardId,
                    false,
                    0,
                    false,
                    revealed,
                    1.0D,
                    chaoticRoll.effects(),
                    chaoticRoll.downsides()
            );
            ItemStack stack = CardItemFactory.module(instance, count);
            giveOrDrop(target, stack);
        }

        context.getSource().sendSuccess(() -> Component.literal("Gave card module " + cardId + " x" + count + " to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int giveBooster(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation setId = ResourceLocationArgument.getId(context, "set");
        int count = getOptionalCount(context);

        if (CardSetManager.get(setId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown card set id: " + setId));
            return 0;
        }

        if (CardBoosterManager.get(setId) == null) {
            context.getSource().sendFailure(Component.literal("No booster is configured for set id: " + setId));
            return 0;
        }

        for (ServerPlayer target : targets) {
            giveOrDrop(target, CardItemFactory.booster(setId, count));
        }

        context.getSource().sendSuccess(() -> Component.literal("Gave booster for set " + setId + " x" + count + " to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int giveSetBooster(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return giveBooster(context);
    }

    private static int giveBoosterBox(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation boxId = ResourceLocationArgument.getId(context, "box");
        int count = getOptionalCount(context);

        if (CardBoosterBoxManager.get(boxId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown booster box id: " + boxId));
            return 0;
        }

        for (ServerPlayer target : targets) {
            giveOrDrop(target, CardItemFactory.boosterBox(boxId, count));
        }

        context.getSource().sendSuccess(() -> Component.literal("Gave booster box " + boxId + " x" + count + " to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int giveCore(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation coreId = ResourceLocationArgument.getId(context, "core");
        int count = getOptionalCount(context);

        if (CardDeckCoreManager.get(coreId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown core id: " + coreId));
            return 0;
        }

        for (ServerPlayer target : targets) {
            giveOrDrop(target, CardItemFactory.deckCore(coreId, count));
        }

        context.getSource().sendSuccess(() -> Component.literal("Gave deck core " + coreId + " x" + count + " to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int giveDeckBox(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation boxId = ResourceLocationArgument.getId(context, "box");
        int count = getOptionalCount(context);

        if (CardDeckBoxManager.get(boxId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown deck box id: " + boxId));
            return 0;
        }

        for (ServerPlayer target : targets) {
            giveOrDrop(target, CardItemFactory.deckBox(boxId, count));
        }

        context.getSource().sendSuccess(() -> Component.literal("Gave deck box " + boxId + " x" + count + " to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int giveSleeve(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int count = getOptionalCount(context);

        for (ServerPlayer target : targets) {
            giveOrDrop(target, new ItemStack(Registration.CARD_SLEEVE_ITEM.get(), count));
        }

        context.getSource().sendSuccess(() -> Component.literal("Gave card sleeve x" + count + " to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int giveTokens(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int count = getOptionalCount(context);

        for (ServerPlayer target : targets) {
            giveOrDrop(target, new ItemStack(Registration.CARD_TOKEN_ITEM.get(), count));
        }

        context.getSource().sendSuccess(() -> Component.literal("Gave card tokens x" + count + " to " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int openBooster(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation setId = ResourceLocationArgument.getId(context, "set");
        int count = getOptionalCount(context);

        if (CardBoosterManager.get(setId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown booster set id: " + setId));
            return 0;
        }

        int opened = 0;
        for (ServerPlayer target : targets) {
            for (int i = 0; i < count; i++) {
                if (CardPackService.openBooster(target, setId)) {
                    opened++;
                }
            }
        }

        int openedSnapshot = opened;
        context.getSource().sendSuccess(() -> Component.literal("Opened booster set " + setId + " " + openedSnapshot + " time(s)."), true);
        return opened;
    }

    private static int assembleDeck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int successes = 0;
        for (ServerPlayer target : targets) {
            if (CardDeckService.assembleDeckFromInventory(target)) {
                successes++;
            }
        }

        int snapshot = successes;
        context.getSource().sendSuccess(() -> Component.literal("Assembled deck for " + snapshot + "/" + targets.size() + " player(s)."), true);
        return successes;
    }

    private static int setIntegrity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            CardDeckService.setIntegrity(target, value);
        }

        context.getSource().sendSuccess(() -> Component.literal("Set equipped deck integrity to " + value + " for " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int setBricked(CommandContext<CommandSourceStack> context, boolean bricked) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            CardDeckService.setBricked(target, bricked);
        }

        context.getSource().sendSuccess(() -> Component.literal((bricked ? "Bricked" : "Unbricked") + " equipped decks for " + targets.size() + " player(s)."), true);
        return targets.size();
    }

    private static int collection(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        CardCollectionService.CollectionSummary summary = CardCollectionService.summary(target);
        context.getSource().sendSuccess(() -> Component.literal(
                target.getGameProfile().getName() + " collection: total=" + summary.totalCards() + ", foils=" + summary.totalFoils() + ", unique=" + summary.counts().size()
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int debugDeck(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        ItemStack stack = CardDeckService.findEquippedDeck(target);
        if (stack == null) {
            context.getSource().sendFailure(Component.literal("Target has no equipped deck in deck Curio slot."));
            return 0;
        }

        CardItemData.DeckData deck = CardItemData.readDeckData(stack);
        if (deck == null) {
            context.getSource().sendFailure(Component.literal("Equipped deck has invalid data."));
            return 0;
        }

        context.getSource().sendSuccess(() -> Component.literal(
                "Deck core=" + deck.coreId() + ", box=" + deck.boxId() + ", integrity=" + deck.integrity() + "/" + deck.maxIntegrity() + ", bricked=" + deck.bricked() + ", modules=" + deck.modules().size()
        ), false);
        return Command.SINGLE_SUCCESS;
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
