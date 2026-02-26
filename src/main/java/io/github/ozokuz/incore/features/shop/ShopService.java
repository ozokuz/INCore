package io.github.ozokuz.incore.features.shop;

import dev.ithundxr.createnumismatics.Numismatics;
import dev.ithundxr.createnumismatics.content.backend.BankAccount;
import io.github.ozokuz.incore.features.gacha.GachaEventRotation;
import io.github.ozokuz.incore.features.market.MarketBanking;
import io.github.ozokuz.incore.features.market.MarketTime;
import io.github.ozokuz.incore.features.shop.network.ShopNetworking;
import io.github.ozokuz.incore.features.tasks.DailyTaskEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ShopService {
    private ShopService() {
    }

    public static void openShopScreen(ServerPlayer player) {
        openShopScreen(player, null, null);
    }

    public static void openShopScreen(
            ServerPlayer player,
            @Nullable ResourceLocation selectedCategoryId,
            @Nullable ResourceLocation selectedOfferId
    ) {
        if (player.getServer() == null) {
            return;
        }
        ShopNetworking.openShopScreen(player, buildScreenData(player, selectedCategoryId, selectedOfferId));
    }

    public static ScreenData buildScreenData(
            ServerPlayer player,
            @Nullable ResourceLocation selectedCategoryId,
            @Nullable ResourceLocation selectedOfferId
    ) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return new ScreenData(0, "", "", List.of(), List.of());
        }

        ShopSavedData data = ShopSavedData.get(server);
        ShopSavedData.PlayerState playerState = data.stateFor(player.getUUID());
        reconcilePlayerState(player, data, playerState);

        List<CategoryView> categories = new ArrayList<>();
        for (ShopCategoryDefinition category : ShopCategoryManager.all()) {
            boolean locked = data.isCategoryLocked(player.getUUID(), category.id());
            int stock = category.stockMode() == ShopStockMode.CATEGORY_BUCKET
                    ? stockForCategoryBucket(data, playerState, category)
                    : -1;

            categories.add(new CategoryView(
                    category.id().toString(),
                    category.displayName(),
                    category.sortOrder(),
                    category.stockMode().serialized(),
                    category.replenishMode().serialized(),
                    stock,
                    locked
            ));
        }

        List<OfferView> offers = new ArrayList<>();
        for (ShopOfferDefinition offer : ShopOfferManager.all()) {
            ShopCategoryDefinition category = ShopCategoryManager.get(offer.categoryId());
            if (category == null) {
                continue;
            }

            boolean categoryLocked = data.isCategoryLocked(player.getUUID(), category.id());
            boolean offerLocked = data.isOfferLocked(player.getUUID(), offer.id());
            boolean locked = categoryLocked || offerLocked;
            int availableStock = stockForOffer(data, playerState, category, offer);

            offers.add(new OfferView(
                    offer.id().toString(),
                    offer.categoryId().toString(),
                    offer.itemId().toString(),
                    offer.displayName(),
                    offer.sortOrder(),
                    offer.priceSpur(),
                    offer.itemCount(),
                    availableStock,
                    locked
            ));
        }
        offers.sort(Comparator
                .comparing((OfferView view) -> {
                    ShopCategoryDefinition category = ShopCategoryManager.get(ResourceLocation.parse(view.categoryId()));
                    return category == null ? Integer.MAX_VALUE : category.sortOrder();
                })
                .thenComparingInt(OfferView::sortOrder)
                .thenComparing(OfferView::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(OfferView::offerId));

        String resolvedCategory = resolveSelectedCategory(categories, offers, selectedCategoryId);
        String resolvedOffer = resolveSelectedOffer(resolvedCategory, offers, selectedOfferId);

        BankAccount account = Numismatics.BANK.getAccount(player);
        int balanceSpur = MarketBanking.balanceSpur(account);
        return new ScreenData(balanceSpur, resolvedCategory, resolvedOffer, categories, offers);
    }

    public static boolean purchase(ServerPlayer player, ResourceLocation offerId, int quantity) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return false;
        }

        ShopOfferDefinition offer = ShopOfferManager.get(offerId);
        if (offer == null) {
            player.sendSystemMessage(Component.translatable("incore.shop.offer_missing"));
            return false;
        }

        ShopCategoryDefinition category = ShopCategoryManager.get(offer.categoryId());
        if (category == null) {
            player.sendSystemMessage(Component.translatable("incore.shop.category_missing"));
            return false;
        }

        ShopSavedData data = ShopSavedData.get(server);
        ShopSavedData.PlayerState playerState = data.stateFor(player.getUUID());
        reconcilePlayerState(player, data, playerState);

        if (isLocked(data, player, category, offer)) {
            player.sendSystemMessage(Component.translatable("incore.shop.locked"));
            return false;
        }

        int purchaseCount = Math.clamp(quantity, 1, 64);
        int availableStock = stockForOffer(data, playerState, category, offer);
        if (availableStock >= 0 && purchaseCount > availableStock) {
            player.sendSystemMessage(Component.translatable("incore.shop.out_of_stock"));
            return false;
        }

        Item item = BuiltInRegistries.ITEM.get(offer.itemId());
        if (item == null || item == Items.AIR) {
            player.sendSystemMessage(Component.translatable("incore.shop.offer_missing"));
            return false;
        }

        int itemsPerPurchase = Math.max(1, offer.itemCount());
        long totalItemsLong = (long) purchaseCount * itemsPerPurchase;
        if (totalItemsLong > Integer.MAX_VALUE) {
            return false;
        }
        int totalItems = (int) totalItemsLong;

        if (!canFit(player, item, totalItems)) {
            player.sendSystemMessage(Component.translatable("incore.shop.inventory_full"));
            return false;
        }

        long totalPriceLong = (long) offer.priceSpur() * purchaseCount;
        if (totalPriceLong > Integer.MAX_VALUE) {
            player.sendSystemMessage(Component.translatable("incore.shop.insufficient_funds"));
            return false;
        }
        int totalPrice = (int) totalPriceLong;

        BankAccount account = Numismatics.BANK.getAccount(player);
        if (account == null) {
            player.sendSystemMessage(Component.translatable("incore.shop.no_account"));
            return false;
        }
        if (!MarketBanking.withdraw(account, totalPrice)) {
            player.sendSystemMessage(Component.translatable("incore.shop.insufficient_funds"));
            return false;
        }

        consumeStock(data, playerState, category, offer, purchaseCount);
        giveItems(player, item, totalItems);
        data.setDirty();
        DailyTaskEvents.onShopPurchase(player);
        return true;
    }

    public static boolean setPlayerCategoryLock(ServerPlayer player, ResourceLocation categoryId, boolean locked) {
        if (ShopCategoryManager.get(categoryId) == null) {
            return false;
        }
        ShopSavedData savedData = ShopSavedData.get(player.getServer());
        return savedData.setPlayerCategoryLock(player.getUUID(), categoryId, locked);
    }

    public static boolean setPlayerOfferLock(ServerPlayer player, ResourceLocation offerId, boolean locked) {
        if (ShopOfferManager.get(offerId) == null) {
            return false;
        }
        ShopSavedData savedData = ShopSavedData.get(player.getServer());
        return savedData.setPlayerOfferLock(player.getUUID(), offerId, locked);
    }

    public static boolean setGlobalCategoryLock(MinecraftServer server, ResourceLocation categoryId, boolean locked) {
        if (ShopCategoryManager.get(categoryId) == null) {
            return false;
        }
        return ShopSavedData.get(server).setGlobalCategoryLock(categoryId, locked);
    }

    public static boolean setGlobalOfferLock(MinecraftServer server, ResourceLocation offerId, boolean locked) {
        if (ShopOfferManager.get(offerId) == null) {
            return false;
        }
        return ShopSavedData.get(server).setGlobalOfferLock(offerId, locked);
    }

    public static int globalLockedCategoryCount(MinecraftServer server) {
        return ShopSavedData.get(server).globalLockedCategories().size();
    }

    public static int globalLockedOfferCount(MinecraftServer server) {
        return ShopSavedData.get(server).globalLockedOffers().size();
    }

    private static String resolveSelectedCategory(
            List<CategoryView> categories,
            List<OfferView> offers,
            @Nullable ResourceLocation requestedCategoryId
    ) {
        if (!categories.isEmpty() && requestedCategoryId != null) {
            String raw = requestedCategoryId.toString();
            if (categories.stream().anyMatch(category -> category.categoryId().equals(raw))) {
                return raw;
            }
        }

        if (!categories.isEmpty()) {
            return categories.getFirst().categoryId();
        }

        if (!offers.isEmpty()) {
            return offers.getFirst().categoryId();
        }

        return "";
    }

    private static String resolveSelectedOffer(
            String resolvedCategoryId,
            List<OfferView> offers,
            @Nullable ResourceLocation requestedOfferId
    ) {
        if (!offers.isEmpty() && requestedOfferId != null) {
            String raw = requestedOfferId.toString();
            if (offers.stream().anyMatch(offer -> offer.offerId().equals(raw))) {
                return raw;
            }
        }

        for (OfferView offer : offers) {
            if (offer.categoryId().equals(resolvedCategoryId)) {
                return offer.offerId();
            }
        }

        return offers.isEmpty() ? "" : offers.getFirst().offerId();
    }

    private static boolean isLocked(
            ShopSavedData savedData,
            ServerPlayer player,
            ShopCategoryDefinition category,
            ShopOfferDefinition offer
    ) {
        return savedData.isCategoryLocked(player.getUUID(), category.id())
                || savedData.isOfferLocked(player.getUUID(), offer.id());
    }

    private static void reconcilePlayerState(
            ServerPlayer player,
            ShopSavedData savedData,
            ShopSavedData.PlayerState playerState
    ) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }

        boolean changed = false;

        for (ShopCategoryDefinition category : ShopCategoryManager.all()) {
            changed |= ensureStockInitialized(playerState, category);

            String token = replenishToken(server, category);
            if (token.isEmpty()) {
                continue;
            }

            String previousToken = playerState.replenishTokens().get(category.id());
            if (previousToken == null) {
                playerState.replenishTokens().put(category.id(), token);
                changed = true;
                continue;
            }

            if (!previousToken.equals(token)) {
                resetStockForCategory(playerState, category);
                playerState.replenishTokens().put(category.id(), token);
                changed = true;
            }
        }

        if (changed) {
            savedData.setDirty();
        }
    }

    private static boolean ensureStockInitialized(ShopSavedData.PlayerState playerState, ShopCategoryDefinition category) {
        return switch (category.stockMode()) {
            case NONE -> false;
            case CATEGORY_BUCKET -> {
                Integer previous = playerState.categoryBucketStocks().putIfAbsent(category.id(), category.initialStock());
                yield previous == null;
            }
            case PER_ITEM -> {
                boolean changed = false;
                for (ShopOfferDefinition offer : ShopOfferManager.byCategory(category.id())) {
                    Integer previous = playerState.itemStocks().putIfAbsent(offer.id(), category.initialStock());
                    if (previous == null) {
                        changed = true;
                    }
                }
                yield changed;
            }
        };
    }

    private static String replenishToken(MinecraftServer server, ShopCategoryDefinition category) {
        return switch (category.replenishMode()) {
            case NONE -> "";
            case DAILY_NOON -> "daily:" + MarketTime.noonDayKey(MarketTime.now(server));
            case GACHA_ROTATION -> {
                if (category.gachaCategoryId() == null) {
                    yield "gacha:missing";
                }
                yield "gacha:" + GachaEventRotation.getRotationTokenForCategory(category.gachaCategoryId());
            }
        };
    }

    private static void resetStockForCategory(ShopSavedData.PlayerState playerState, ShopCategoryDefinition category) {
        switch (category.stockMode()) {
            case NONE -> {
            }
            case CATEGORY_BUCKET -> playerState.categoryBucketStocks().put(category.id(), category.initialStock());
            case PER_ITEM -> {
                for (ShopOfferDefinition offer : ShopOfferManager.byCategory(category.id())) {
                    playerState.itemStocks().put(offer.id(), category.initialStock());
                }
            }
        }
    }

    private static int stockForOffer(
            ShopSavedData savedData,
            ShopSavedData.PlayerState playerState,
            ShopCategoryDefinition category,
            ShopOfferDefinition offer
    ) {
        return switch (category.stockMode()) {
            case NONE -> -1;
            case CATEGORY_BUCKET -> stockForCategoryBucket(savedData, playerState, category);
            case PER_ITEM -> {
                Integer stock = playerState.itemStocks().get(offer.id());
                if (stock == null) {
                    stock = category.initialStock();
                    playerState.itemStocks().put(offer.id(), stock);
                    savedData.setDirty();
                }
                yield Math.max(0, stock);
            }
        };
    }

    private static int stockForCategoryBucket(
            ShopSavedData savedData,
            ShopSavedData.PlayerState playerState,
            ShopCategoryDefinition category
    ) {
        Integer stock = playerState.categoryBucketStocks().get(category.id());
        if (stock == null) {
            stock = category.initialStock();
            playerState.categoryBucketStocks().put(category.id(), stock);
            savedData.setDirty();
        }
        return Math.max(0, stock);
    }

    private static void consumeStock(
            ShopSavedData savedData,
            ShopSavedData.PlayerState playerState,
            ShopCategoryDefinition category,
            ShopOfferDefinition offer,
            int quantity
    ) {
        switch (category.stockMode()) {
            case NONE -> {
            }
            case CATEGORY_BUCKET -> {
                int current = stockForCategoryBucket(savedData, playerState, category);
                playerState.categoryBucketStocks().put(category.id(), Math.max(0, current - quantity));
            }
            case PER_ITEM -> {
                int current = stockForOffer(savedData, playerState, category, offer);
                playerState.itemStocks().put(offer.id(), Math.max(0, current - quantity));
            }
        }
    }

    private static boolean canFit(ServerPlayer player, Item item, int totalItems) {
        int remaining = totalItems;
        int maxStack = Math.max(1, item.getDefaultMaxStackSize());
        ItemStack reference = new ItemStack(item);

        for (ItemStack stack : player.getInventory().items) {
            if (remaining <= 0) {
                return true;
            }

            if (stack.isEmpty()) {
                remaining -= maxStack;
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(stack, reference)) {
                continue;
            }

            remaining -= Math.max(0, maxStack - stack.getCount());
        }

        return remaining <= 0;
    }

    private static void giveItems(ServerPlayer player, Item item, int totalItems) {
        int remaining = totalItems;
        int maxStack = Math.max(1, item.getDefaultMaxStackSize());

        while (remaining > 0) {
            int giving = Math.min(remaining, maxStack);
            ItemStack stack = new ItemStack(item, giving);
            if (!player.addItem(stack)) {
                player.drop(stack, false);
            }
            remaining -= giving;
        }
    }

    public record ScreenData(
            int balanceSpur,
            String selectedCategoryId,
            String selectedOfferId,
            List<CategoryView> categories,
            List<OfferView> offers
    ) {
    }

    public record CategoryView(
            String categoryId,
            String displayName,
            int sortOrder,
            String stockMode,
            String replenishMode,
            int availableStock,
            boolean locked
    ) {
    }

    public record OfferView(
            String offerId,
            String categoryId,
            String itemId,
            String displayName,
            int sortOrder,
            int priceSpur,
            int itemCount,
            int availableStock,
            boolean locked
    ) {
    }
}
