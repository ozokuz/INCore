package ozokuz.incore.features.shop.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import ozokuz.incore.features.shop.ShopCategoryManager;
import ozokuz.incore.features.shop.ShopOfferManager;
import ozokuz.incore.features.shop.ShopService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Collection;

public final class ShopCommands {
    private static final SuggestionProvider<CommandSourceStack> CATEGORY_ID_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(ShopCategoryManager.ids(), builder);

    private static final SuggestionProvider<CommandSourceStack> OFFER_ID_SUGGESTIONS =
            (context, builder) -> SharedSuggestionProvider.suggestResource(ShopOfferManager.ids(), builder);

    private ShopCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("incore")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("shop")
                                .then(Commands.literal("status")
                                        .executes(ShopCommands::status))
                                .then(Commands.literal("sync")
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ShopCommands::sync)))
                                .then(Commands.literal("lock")
                                        .then(Commands.literal("category")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("category", ResourceLocationArgument.id())
                                                                .suggests(CATEGORY_ID_SUGGESTIONS)
                                                                .executes(context -> lockCategory(context, true)))))
                                        .then(Commands.literal("offer")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("offer", ResourceLocationArgument.id())
                                                                .suggests(OFFER_ID_SUGGESTIONS)
                                                                .executes(context -> lockOffer(context, true))))))
                                .then(Commands.literal("unlock")
                                        .then(Commands.literal("category")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("category", ResourceLocationArgument.id())
                                                                .suggests(CATEGORY_ID_SUGGESTIONS)
                                                                .executes(context -> lockCategory(context, false)))))
                                        .then(Commands.literal("offer")
                                                .then(Commands.argument("targets", EntityArgument.players())
                                                        .then(Commands.argument("offer", ResourceLocationArgument.id())
                                                                .suggests(OFFER_ID_SUGGESTIONS)
                                                                .executes(context -> lockOffer(context, false))))))
                                .then(Commands.literal("lock_global")
                                        .then(Commands.literal("category")
                                                .then(Commands.argument("category", ResourceLocationArgument.id())
                                                        .suggests(CATEGORY_ID_SUGGESTIONS)
                                                        .executes(context -> lockGlobalCategory(context, true))))
                                        .then(Commands.literal("offer")
                                                .then(Commands.argument("offer", ResourceLocationArgument.id())
                                                        .suggests(OFFER_ID_SUGGESTIONS)
                                                        .executes(context -> lockGlobalOffer(context, true)))))
                                .then(Commands.literal("unlock_global")
                                        .then(Commands.literal("category")
                                                .then(Commands.argument("category", ResourceLocationArgument.id())
                                                        .suggests(CATEGORY_ID_SUGGESTIONS)
                                                        .executes(context -> lockGlobalCategory(context, false))))
                                        .then(Commands.literal("offer")
                                                .then(Commands.argument("offer", ResourceLocationArgument.id())
                                                        .suggests(OFFER_ID_SUGGESTIONS)
                                                        .executes(context -> lockGlobalOffer(context, false))))))
        );
    }

    private static int status(CommandContext<CommandSourceStack> context) {
        int categoryCount = ShopCategoryManager.all().size();
        int offerCount = ShopOfferManager.all().size();
        int globalCategoryLocks = ShopService.globalLockedCategoryCount(context.getSource().getServer());
        int globalOfferLocks = ShopService.globalLockedOfferCount(context.getSource().getServer());

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Shop definitions -> categories=" + categoryCount
                                + ", offers=" + offerCount
                                + ", globalCategoryLocks=" + globalCategoryLocks
                                + ", globalOfferLocks=" + globalOfferLocks
                ),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int sync(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        for (ServerPlayer target : targets) {
            ShopService.openShopScreen(target);
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Synced shop screen to " + targets.size() + " player(s)."),
                true
        );
        return targets.size();
    }

    private static int lockCategory(CommandContext<CommandSourceStack> context, boolean locked) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation categoryId = ResourceLocationArgument.getId(context, "category");

        int changed = 0;
        for (ServerPlayer target : targets) {
            if (ShopService.setPlayerCategoryLock(target, categoryId, locked)) {
                changed++;
            }
            ShopService.openShopScreen(target, categoryId, null);
        }

        String action = locked ? "Locked" : "Unlocked";
        int changedCount = changed;
        context.getSource().sendSuccess(
                () -> Component.literal(action + " category " + categoryId + " for " + changedCount + " player(s)."),
                true
        );
        return changed;
    }

    private static int lockOffer(CommandContext<CommandSourceStack> context, boolean locked) throws CommandSyntaxException {
        Collection<ServerPlayer> targets = EntityArgument.getPlayers(context, "targets");
        ResourceLocation offerId = ResourceLocationArgument.getId(context, "offer");

        int changed = 0;
        for (ServerPlayer target : targets) {
            if (ShopService.setPlayerOfferLock(target, offerId, locked)) {
                changed++;
            }
            ShopService.openShopScreen(target, null, offerId);
        }

        String action = locked ? "Locked" : "Unlocked";
        int changedCount = changed;
        context.getSource().sendSuccess(
                () -> Component.literal(action + " offer " + offerId + " for " + changedCount + " player(s)."),
                true
        );
        return changed;
    }

    private static int lockGlobalCategory(CommandContext<CommandSourceStack> context, boolean locked) {
        ResourceLocation categoryId = ResourceLocationArgument.getId(context, "category");
        boolean changed = ShopService.setGlobalCategoryLock(context.getSource().getServer(), categoryId, locked);
        if (!changed) {
            context.getSource().sendFailure(Component.literal("Failed to update global category lock for " + categoryId + "."));
            return 0;
        }

        String action = locked ? "Locked" : "Unlocked";
        context.getSource().sendSuccess(
                () -> Component.literal(action + " global category " + categoryId + "."),
                true
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int lockGlobalOffer(CommandContext<CommandSourceStack> context, boolean locked) {
        ResourceLocation offerId = ResourceLocationArgument.getId(context, "offer");
        boolean changed = ShopService.setGlobalOfferLock(context.getSource().getServer(), offerId, locked);
        if (!changed) {
            context.getSource().sendFailure(Component.literal("Failed to update global offer lock for " + offerId + "."));
            return 0;
        }

        String action = locked ? "Locked" : "Unlocked";
        context.getSource().sendSuccess(
                () -> Component.literal(action + " global offer " + offerId + "."),
                true
        );
        return Command.SINGLE_SUCCESS;
    }
}
