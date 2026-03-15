package ozokuz.incore.features.gacha.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import ozokuz.incore.Registration;
import ozokuz.incore.features.gacha.GachaBannerData;
import ozokuz.incore.features.gacha.GachaBannerManager;
import ozokuz.incore.features.gacha.GachaEventCategoryManager;
import ozokuz.incore.features.gacha.GachaEventRotation;
import ozokuz.incore.features.gacha.GachaService;
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

public final class GachaCommands {
    private static final SuggestionProvider<CommandSourceStack> BANNER_ID_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    GachaBannerManager.all().stream().map(GachaBannerData::id).toList(),
                    builder
            );
    private static final SuggestionProvider<CommandSourceStack> CATEGORY_ID_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(
                    GachaEventRotation.getKnownCategoryIds(),
                    builder
            );

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
                                                        .suggests(BANNER_ID_SUGGESTIONS)
                                                        .executes(GachaCommands::buyBanner))))
                                .then(Commands.literal("give_basic_permit")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(GachaCommands::giveBasicPermit))))
                                .then(Commands.literal("give_chartered_permit")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .executes(GachaCommands::giveCharteredPermit))))
                                .then(Commands.literal("give_time_piece")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .then(Commands.argument("banner", ResourceLocationArgument.id())
                                                        .suggests(BANNER_ID_SUGGESTIONS)
                                                        .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(GachaCommands::giveBannerPermit)))))
                                .then(Commands.literal("pity")
                                        .then(Commands.literal("set")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("banner", ResourceLocationArgument.id())
                                                                .suggests(BANNER_ID_SUGGESTIONS)
                                                                .then(Commands.argument("pity_five", IntegerArgumentType.integer(0))
                                                                        .then(Commands.argument("pity_six", IntegerArgumentType.integer(0))
                                                                                .then(Commands.argument("featured_six", IntegerArgumentType.integer(0))
                                                                                        .then(Commands.argument("basic_selected_six", IntegerArgumentType.integer(0))
                                                                                                .executes(GachaCommands::setPity))))))))
                                        .then(Commands.literal("status")
                                                .then(Commands.argument("target", EntityArgument.player())
                                                        .then(Commands.argument("banner", ResourceLocationArgument.id())
                                                                .suggests(BANNER_ID_SUGGESTIONS)
                                                                .executes(GachaCommands::statusPity)))))
                                .then(Commands.literal("rotate")
                                        .then(Commands.argument("category", ResourceLocationArgument.id())
                                                .suggests(CATEGORY_ID_SUGGESTIONS)
                                                .then(Commands.literal("next")
                                                        .executes(context -> rotateCategory(context, 1)))
                                                .then(Commands.literal("previous")
                                                        .executes(context -> rotateCategory(context, -1))))))
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
            giveOrDrop(target, new ItemStack(Registration.BASIC_TIME_PIECE_ITEM.get(), count));
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
            giveOrDrop(target, new ItemStack(Registration.CHARTERED_TIME_PIECE_ITEM.get(), count));
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

    private static int setPity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation bannerId = ResourceLocationArgument.getId(context, "banner");
        if (bannerId == null || GachaBannerManager.get(bannerId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown banner id."));
            return 0;
        }

        int pityFive = IntegerArgumentType.getInteger(context, "pity_five");
        int pitySix = IntegerArgumentType.getInteger(context, "pity_six");
        int featuredSix = IntegerArgumentType.getInteger(context, "featured_six");
        int basicSelectedSix = IntegerArgumentType.getInteger(context, "basic_selected_six");

        int updated = 0;
        for (ServerPlayer target : targets) {
            if (GachaService.setPityForBanner(target, bannerId, pityFive, pitySix, featuredSix, basicSelectedSix)) {
                updated++;
            }
        }

        if (updated <= 0) {
            context.getSource().sendFailure(Component.literal("Failed to set pity values."));
            return 0;
        }

        int finalUpdated = updated;
        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Set pity for " + bannerId
                                + " on " + finalUpdated + " player(s): "
                                + "5*=" + pityFive
                                + ", 6*=" + pitySix
                                + ", featured6=" + featuredSix
                                + ", basic240=" + basicSelectedSix
                ),
                true
        );
        return finalUpdated;
    }

    private static int statusPity(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        ResourceLocation bannerId = ResourceLocationArgument.getId(context, "banner");
        if (bannerId == null || GachaBannerManager.get(bannerId) == null) {
            context.getSource().sendFailure(Component.literal("Unknown banner id."));
            return 0;
        }

        GachaService.PityView pity = GachaService.getPityForBanner(target, bannerId);
        if (pity == null) {
            context.getSource().sendFailure(Component.literal("No pity data available."));
            return 0;
        }

        context.getSource().sendSuccess(
                () -> Component.literal(
                        target.getGameProfile().getName() + " pity for " + bannerId + ": "
                                + "5*=" + pity.pityFive()
                                + ", 6*=" + pity.pitySix()
                                + ", featured6=" + pity.eventFeaturedPity()
                                + ", basic240=" + pity.basicSelectedSixPity()
                                + ", token=" + (pity.eventRotationToken() == null ? "none" : pity.eventRotationToken())
                ),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int rotateCategory(CommandContext<CommandSourceStack> context, int direction) {
        CommandSourceStack source = context.getSource();
        ResourceLocation categoryId = ResourceLocationArgument.getId(context, "category");
        if (categoryId == null || GachaEventCategoryManager.get(categoryId) == null) {
            source.sendFailure(Component.literal("Unknown gacha category id."));
            return 0;
        }

        return GachaEventRotation.rotateCategory(categoryId, direction)
                .map(result -> {
                    String action = direction >= 0 ? "next" : "previous";
                    String remaining = formatDuration(result.remainingMillis());
                    source.sendSuccess(
                            () -> Component.literal(
                                    "Rotated " + categoryId + " to " + action + " banner: "
                                            + result.bannerId() + " (time left this window: " + remaining + ")"
                            ),
                            true
                    );
                    return Command.SINGLE_SUCCESS;
                })
                .orElseGet(() -> {
                    source.sendFailure(Component.literal("Category has no valid event banners."));
                    return 0;
                });
    }

    private static String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
