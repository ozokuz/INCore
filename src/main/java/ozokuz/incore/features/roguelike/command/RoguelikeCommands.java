package ozokuz.incore.features.roguelike.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import ozokuz.incore.INCore;
import ozokuz.incore.features.roguelike.RoguelikeService;
import ozokuz.incore.features.roguelike.data.DungeonObjectiveManager;
import ozokuz.incore.features.roguelike.data.DungeonThemeManager;
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

public final class RoguelikeCommands {
    private static final String RANDOM_TOKEN = "random";
    private static final DynamicCommandExceptionType MISSING_THEME = new DynamicCommandExceptionType(
            value -> Component.translatable("incore.command.roguelike.theme_missing", value)
    );
    private static final DynamicCommandExceptionType MISSING_OBJECTIVE = new DynamicCommandExceptionType(
            value -> Component.translatable("incore.command.roguelike.objective_missing", value)
    );

    private static final SuggestionProvider<CommandSourceStack> THEME_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(DungeonThemeManager.THEMES.keySet(), builder);
    private static final SuggestionProvider<CommandSourceStack> OBJECTIVE_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(DungeonObjectiveManager.OBJECTIVES.keySet(), builder);

    private RoguelikeCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("roguelike")
                                .then(altarDifficultyCommands())
                                .then(crystalCommands()))
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> altarDifficultyCommands() {
        return Commands.literal("altar_difficulty")
                .then(Commands.literal("get")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(RoguelikeCommands::getDifficulty)))
                .then(Commands.literal("set")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0))
                                        .executes(RoguelikeCommands::setDifficulty))))
                .then(Commands.literal("add")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(RoguelikeCommands::addDifficulty))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> crystalCommands() {
        var crystal = Commands.literal("crystal");
        var create = Commands.literal("create");
        var targets = Commands.argument("targets", EntityArgument.players());

        var randomTheme = Commands.literal(RANDOM_TOKEN);
        var randomRandom = Commands.literal(RANDOM_TOKEN)
                .executes(ctx -> createCrystal(ctx, 1, null, null))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> createCrystal(ctx, IntegerArgumentType.getInteger(ctx, "amount"), null, null)));
        var randomObjective = Commands.argument("objective", ResourceLocationArgument.id())
                .suggests(OBJECTIVE_SUGGESTIONS)
                .executes(ctx -> createCrystal(ctx, 1, null, resolveObjectiveId(ResourceLocationArgument.getId(ctx, "objective"))))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> createCrystal(ctx, IntegerArgumentType.getInteger(ctx, "amount"), null, resolveObjectiveId(ResourceLocationArgument.getId(ctx, "objective")))));
        randomTheme.then(randomRandom);
        randomTheme.then(randomObjective);

        var theme = Commands.argument("theme", ResourceLocationArgument.id())
                .suggests(THEME_SUGGESTIONS);
        var themeRandomObjective = Commands.literal(RANDOM_TOKEN)
                .executes(ctx -> createCrystal(ctx, 1, resolveThemeId(ResourceLocationArgument.getId(ctx, "theme")), null))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> createCrystal(ctx, IntegerArgumentType.getInteger(ctx, "amount"), resolveThemeId(ResourceLocationArgument.getId(ctx, "theme")), null)));
        var themeObjective = Commands.argument("objective", ResourceLocationArgument.id())
                .suggests(OBJECTIVE_SUGGESTIONS)
                .executes(ctx -> createCrystal(ctx, 1, resolveThemeId(ResourceLocationArgument.getId(ctx, "theme")), resolveObjectiveId(ResourceLocationArgument.getId(ctx, "objective"))))
                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                        .executes(ctx -> createCrystal(ctx, IntegerArgumentType.getInteger(ctx, "amount"), resolveThemeId(ResourceLocationArgument.getId(ctx, "theme")), resolveObjectiveId(ResourceLocationArgument.getId(ctx, "objective")))));
        theme.then(themeRandomObjective);
        theme.then(themeObjective);

        targets.then(randomTheme);
        targets.then(theme);
        create.then(targets);
        crystal.then(create);
        return crystal;
    }

    private static int getDifficulty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        int value = RoguelikeService.getAltarDifficulty(context.getSource().getServer(), target.getUUID());

        context.getSource().sendSuccess(
                () -> Component.translatable("incore.command.roguelike.altar_difficulty.get", target.getGameProfile().getName(), value),
                false
        );

        return Command.SINGLE_SUCCESS;
    }

    private static int setDifficulty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int value = IntegerArgumentType.getInteger(context, "value");

        for (ServerPlayer target : targets) {
            RoguelikeService.setAltarDifficulty(context.getSource().getServer(), target.getUUID(), value);
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("incore.command.roguelike.altar_difficulty.set", value, targets.size()),
                true
        );

        return targets.size();
    }

    private static int addDifficulty(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        for (ServerPlayer target : targets) {
            int current = RoguelikeService.getAltarDifficulty(context.getSource().getServer(), target.getUUID());
            int updated = Math.max(0, current + amount);
            RoguelikeService.setAltarDifficulty(context.getSource().getServer(), target.getUUID(), updated);
        }

        context.getSource().sendSuccess(
                () -> Component.translatable("incore.command.roguelike.altar_difficulty.add", amount, targets.size()),
                true
        );

        return targets.size();
    }

    private static int createCrystal(CommandContext<CommandSourceStack> context, int amount, ResourceLocation themeId, ResourceLocation objectiveId) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");

        for (ServerPlayer target : targets) {
            ItemStack stack = RoguelikeService.createDungeonCrystal(amount, themeId, objectiveId);
            if (!target.addItem(stack)) {
                target.drop(stack, false);
            }
        }

        Component details = (themeId == null && objectiveId == null)
                ? Component.translatable("incore.command.roguelike.crystal.details.random")
                : Component.translatable(
                        "incore.command.roguelike.crystal.details.custom",
                        themeId == null ? Component.translatable("incore.roguelike.crystal.tooltip.random") : RoguelikeService.themeDisplayName(themeId),
                        objectiveId == null ? Component.translatable("incore.roguelike.crystal.tooltip.random") : RoguelikeService.objectiveDisplayName(objectiveId)
                );

        int finalAmount = amount;
        context.getSource().sendSuccess(
                () -> Component.translatable("incore.command.roguelike.crystal.give", finalAmount, targets.size()).append(" ").append(details),
                true
        );

        return targets.size();
    }

    private static ResourceLocation resolveThemeId(ResourceLocation id) throws CommandSyntaxException {
        if (DungeonThemeManager.THEMES.containsKey(id)) {
            return id;
        }

        if ("minecraft".equals(id.getNamespace())) {
            ResourceLocation modScoped = ResourceLocation.fromNamespaceAndPath(INCore.MODID, id.getPath());
            if (DungeonThemeManager.THEMES.containsKey(modScoped)) {
                return modScoped;
            }
        }

        throw MISSING_THEME.create(id.toString());
    }

    private static ResourceLocation resolveObjectiveId(ResourceLocation id) throws CommandSyntaxException {
        if (DungeonObjectiveManager.OBJECTIVES.containsKey(id)) {
            return id;
        }

        if ("minecraft".equals(id.getNamespace())) {
            ResourceLocation modScoped = ResourceLocation.fromNamespaceAndPath(INCore.MODID, id.getPath());
            if (DungeonObjectiveManager.OBJECTIVES.containsKey(modScoped)) {
                return modScoped;
            }
        }

        throw MISSING_OBJECTIVE.create(id.toString());
    }
}
