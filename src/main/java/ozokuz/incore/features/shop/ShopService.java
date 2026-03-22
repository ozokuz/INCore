package ozokuz.incore.features.shop;

import com.google.gson.Gson;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;
import ozokuz.incore.features.market.MarketTime;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import ozokuz.incore.features.tasks.DailyTaskEvents;
import ozokuz.incore.features.vendingmachine.VendingMachineItemUtil;
import ozokuz.incore.integration.ldlib.ui.INCorePlayerUiNavigator;
import ozokuz.incore.integration.ldlib.ui.INCoreUiIds;
import ozokuz.incore.integration.ldlib.ui.INCoreUiRouteContext;
import ozokuz.incore.integration.ldlib.ui.ShopUiRouteContext;

public final class ShopService {
    private static final Gson GSON = new Gson();
    private static final long UI_TIMER_BUCKET_MILLIS = 60_000L;

    private ShopService() {
    }

    public static void openShopApp(ServerPlayer player) {
        openShopApp(player, null, null);
    }

    public static void openShopApp(
            ServerPlayer player,
            @Nullable ResourceLocation selectedCategoryId,
            @Nullable ResourceLocation selectedOfferId
    ) {
        INCorePlayerUiNavigator.openRoot(
                player,
                INCoreUiIds.SHOP_APP,
                new ShopUiRouteContext(selectedCategoryId, selectedOfferId)
        );
    }

    public static String buildScreenJson(ServerPlayer player) {
        RequestedSelection selection = requestedSelection(player, null, null);
        return GSON.toJson(buildScreenData(player, selection.categoryId(), selection.offerId()));
    }

    public static ScreenData buildScreenData(
            ServerPlayer player,
            @Nullable ResourceLocation selectedCategoryId,
            @Nullable ResourceLocation selectedOfferId
    ) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return new ScreenData("", "", List.of(), List.of(), List.of());
        }

        ShopSavedData data = ShopSavedData.get(server);
        ShopSavedData.PlayerState playerState = data.stateFor(player.getUUID());
        reconcilePlayerState(player, data, playerState);

        long now = System.currentTimeMillis();
        List<CategoryView> categories = new ArrayList<>();
        List<OfferView> offers = new ArrayList<>();

        for (ShopCategoryDefinition category : ShopCategoryManager.all()) {
            boolean locked = isCategoryLocked(data, player, category.id());
            int stock = category.stockMode() == ShopStockMode.CATEGORY_BUCKET
                    ? stockForCategoryBucket(data, playerState, category)
                    : -1;

            List<VisibleOffer> visibleOffers = visibleOffersForCategory(now, category);
            categories.add(new CategoryView(
                    category.id().toString(),
                    category.displayName(),
                    category.stockMode().serialized(),
                    category.replenishMode().serialized(),
                    stock,
                    locked,
                    currencyView(player, category.defaultCurrency(), 1),
                    category.rotation() != null,
                    category.rotation() == null ? -1L : uiRemainingMillis(remainingMillisForCurrentStep(now, category.rotation())),
                    visibleOffers.size()
            ));

            for (VisibleOffer visibleOffer : visibleOffers) {
                ShopOfferDefinition offer = visibleOffer.offer();
                boolean offerLocked = data.isOfferLocked(player.getUUID(), offer.id());
                int availableStock = stockForOffer(data, playerState, category, offer);
                offers.add(new OfferView(
                        offer.id().toString(),
                        category.id().toString(),
                        offer.displayName(),
                        offer.price(),
                        currencyView(player, effectiveCurrencySpec(category, offer), offer.price()),
                        rewardEntryViews(offer.purchaseable()),
                        availableStock,
                        locked || offerLocked,
                        uiRemainingMillis(visibleOffer.rotationRemainingMillis())
                ));
            }
        }

        List<TabView> tabs = buildTabViews();
        String resolvedCategory = resolveSelectedCategory(tabs, categories, offers, selectedCategoryId);
        String resolvedOffer = resolveSelectedOffer(tabs, categories, offers, resolvedCategory, selectedOfferId);
        return new ScreenData(resolvedCategory, resolvedOffer, tabs, categories, offers);
    }

    public static List<CategoryView> orderedCategoriesForTab(ScreenData data, ShopTabId tabId) {
        TabView tab = findTab(data, tabId);
        if (tab == null) {
            return List.of();
        }
        List<CategoryView> ordered = new ArrayList<>();
        for (String categoryId : tab.categoryIds()) {
            CategoryView category = findCategory(data, categoryId);
            if (category != null) {
                ordered.add(category);
            }
        }
        return List.copyOf(ordered);
    }

    public static TabFeedView buildTabFeed(ScreenData data, ShopTabId tabId, @Nullable String requestedCategoryId) {
        List<CategoryView> orderedCategories = orderedCategoriesForTab(data, tabId);
        if (orderedCategories.isEmpty()) {
            return new TabFeedView("", List.of(), List.of(), List.of());
        }

        String activeCategoryId = orderedCategories.stream()
                .map(CategoryView::categoryId)
                .filter(categoryId -> categoryId.equals(requestedCategoryId))
                .findFirst()
                .orElseGet(() -> orderedCategories.getFirst().categoryId());

        List<OfferView> primaryFeed = offersForCategory(data, activeCategoryId);
        TabView tab = findTab(data, tabId);
        if (tab == null || !tab.showcase().enabled()) {
            return new TabFeedView(
                    activeCategoryId,
                    orderedCategories.stream().map(CategoryView::categoryId).toList(),
                    List.of(),
                    primaryFeed
            );
        }

        List<OfferView> showcase = selectShowcaseOffers(data, tab, orderedCategories, activeCategoryId, primaryFeed);
        Set<String> showcasedIds = showcase.stream().map(OfferView::offerId).collect(java.util.stream.Collectors.toCollection(HashSet::new));
        List<OfferView> remaining = primaryFeed.stream()
                .filter(offer -> !showcasedIds.contains(offer.offerId()))
                .toList();
        return new TabFeedView(
                activeCategoryId,
                orderedCategories.stream().map(CategoryView::categoryId).toList(),
                showcase,
                remaining
        );
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

        if (!isOfferCurrentlyVisible(category, offer.id(), System.currentTimeMillis())) {
            player.sendSystemMessage(Component.translatable("incore.shop.offer_missing"));
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

        List<ItemStack> grantedStacks = resolveGrantedStacks(offer.purchaseable(), purchaseCount);
        if (grantedStacks.isEmpty() || !canFitAll(player, grantedStacks)) {
            player.sendSystemMessage(Component.translatable("incore.shop.inventory_full"));
            return false;
        }

        long totalPriceLong = (long) offer.price() * purchaseCount;
        if (totalPriceLong <= 0L || totalPriceLong > Integer.MAX_VALUE) {
            player.sendSystemMessage(Component.translatable("incore.shop.insufficient_funds"));
            return false;
        }
        int totalPrice = (int) totalPriceLong;

        ShopCurrencySpec effectiveCurrency = effectiveCurrencySpec(category, offer);
        ShopCurrencyType currencyType = ShopCurrencyRegistry.get(effectiveCurrency.typeId());
        if (currencyType == null || currencyType.availableAmount(player, effectiveCurrency) < totalPrice) {
            player.sendSystemMessage(Component.translatable("incore.shop.insufficient_funds"));
            return false;
        }
        if (!currencyType.consume(player, effectiveCurrency, totalPrice)) {
            player.sendSystemMessage(Component.translatable("incore.shop.insufficient_funds"));
            return false;
        }

        consumeStock(data, playerState, category, offer, purchaseCount);
        giveOrDropAll(player, grantedStacks);
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

    public static @Nullable ItemStack parseStack(String stackSpec, int count) {
        ResourceLocation itemId = ResourceLocation.tryParse(stackSpec);
        if (itemId == null) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null || item == Items.AIR) {
            return null;
        }
        return new ItemStack(item, Math.max(1, count));
    }

    static List<ResourceLocation> visibleOfferIdsForCategory(ShopCategoryDefinition category, long now) {
        return visibleOffersForCategory(now, category).stream()
                .map(visibleOffer -> visibleOffer.offer().id())
                .toList();
    }

    static List<ResourceLocation> visibleOfferIdsForOrderedOffers(
            List<ResourceLocation> orderedOfferIds,
            ShopCategoryRotationDefinition rotation,
            long now
    ) {
        if (orderedOfferIds.isEmpty()) {
            return List.of();
        }
        int offerCount = orderedOfferIds.size();
        int visibleCount = Math.min(rotation.visibleCount(), offerCount);
        int start = Math.floorMod((int) rotationStep(now, rotation), offerCount);
        List<ResourceLocation> visible = new ArrayList<>(visibleCount);
        for (int offset = 0; offset < visibleCount; offset++) {
            visible.add(orderedOfferIds.get(Math.floorMod(start + offset, offerCount)));
        }
        return List.copyOf(visible);
    }

    static long rotationRemainingMillisForOffer(ShopCategoryDefinition category, ResourceLocation offerId, long now) {
        for (VisibleOffer visibleOffer : visibleOffersForCategory(now, category)) {
            if (visibleOffer.offer().id().equals(offerId)) {
                return visibleOffer.rotationRemainingMillis();
            }
        }
        return -1L;
    }

    static RequestedSelection requestedSelection(
            ServerPlayer player,
            @Nullable ResourceLocation requestedCategoryId,
            @Nullable ResourceLocation requestedOfferId
    ) {
        INCoreUiRouteContext context = INCorePlayerUiNavigator.current(player)
                .filter(entry -> INCoreUiIds.SHOP_APP.equals(entry.routeId()))
                .map(entry -> entry.context())
                .orElse(INCoreUiRouteContext.Empty.INSTANCE);
        return requestedSelectionForContext(requestedCategoryId, requestedOfferId, context);
    }

    static RequestedSelection requestedSelectionForContext(
            @Nullable ResourceLocation requestedCategoryId,
            @Nullable ResourceLocation requestedOfferId,
            INCoreUiRouteContext context
    ) {
        if (requestedCategoryId != null || requestedOfferId != null) {
            return new RequestedSelection(requestedCategoryId, requestedOfferId);
        }
        if (context instanceof ShopUiRouteContext shopContext) {
            return new RequestedSelection(shopContext.selectedCategoryId(), shopContext.selectedOfferId());
        }
        return new RequestedSelection(null, null);
    }

    private static List<TabView> buildTabViews() {
        List<TabView> tabs = new ArrayList<>();
        for (ShopTabDefinition definition : ShopTabManager.all()) {
            tabs.add(new TabView(
                    definition.id().serialized(),
                    definition.displayName(),
                    definition.paletteId().serialized(),
                    definition.layoutId().serialized(),
                    definition.categoryNavigationMode().serialized(),
                    definition.detailsMode().serialized(),
                    definition.categoryIds().stream().map(ResourceLocation::toString).toList(),
                    new ShowcaseView(
                            definition.showcase().enabled(),
                            definition.showcase().slots(),
                            definition.showcase().source().serialized(),
                            definition.showcase().categoryScope().stream().map(ResourceLocation::toString).toList()
                    )
            ));
        }
        return List.copyOf(tabs);
    }

    private static String resolveSelectedCategory(
            List<TabView> tabs,
            List<CategoryView> categories,
            List<OfferView> offers,
            @Nullable ResourceLocation requestedCategoryId
    ) {
        if (requestedCategoryId != null) {
            String raw = requestedCategoryId.toString();
            if (categories.stream().anyMatch(category -> category.categoryId().equals(raw))) {
                return raw;
            }
        }

        for (TabView tab : tabs) {
            for (String categoryId : tab.categoryIds()) {
                if (categories.stream().anyMatch(category -> category.categoryId().equals(categoryId))) {
                    return categoryId;
                }
            }
        }

        if (!offers.isEmpty()) {
            return offers.getFirst().categoryId();
        }
        return "";
    }

    private static String resolveSelectedOffer(
            List<TabView> tabs,
            List<CategoryView> categories,
            List<OfferView> offers,
            String resolvedCategoryId,
            @Nullable ResourceLocation requestedOfferId
    ) {
        if (requestedOfferId != null) {
            String raw = requestedOfferId.toString();
            if (offers.stream().anyMatch(offer -> offer.offerId().equals(raw))) {
                return raw;
            }
        }

        ShopTabId tabId = null;
        for (TabView tab : tabs) {
            if (tab.categoryIds().contains(resolvedCategoryId)) {
                tabId = ShopTabId.fromString(tab.tabId());
                break;
            }
        }
        if (tabId != null) {
            TabFeedView feed = buildTabFeed(new ScreenData(resolvedCategoryId, "", tabs, categories, offers), tabId, resolvedCategoryId);
            List<OfferView> displayOffers = !feed.showcaseOffers().isEmpty() ? feed.showcaseOffers() : feed.remainingOffers();
            if (!displayOffers.isEmpty()) {
                return displayOffers.getFirst().offerId();
            }
        }

        for (OfferView offer : offers) {
            if (offer.categoryId().equals(resolvedCategoryId)) {
                return offer.offerId();
            }
        }
        return "";
    }

    private static boolean isLocked(
            ShopSavedData savedData,
            ServerPlayer player,
            ShopCategoryDefinition category,
            ShopOfferDefinition offer
    ) {
        return isCategoryLocked(savedData, player, category.id())
                || savedData.isOfferLocked(player.getUUID(), offer.id());
    }

    private static boolean isCategoryLocked(ShopSavedData savedData, ServerPlayer player, ResourceLocation categoryId) {
        ResourceLocation unlockId = requiredUnlockForCategory(categoryId);
        return savedData.isCategoryLocked(player.getUUID(), categoryId)
                || (unlockId != null && !PlayerFeatureUnlockService.hasUnlocked(player, unlockId));
    }

    private static @Nullable ResourceLocation requiredUnlockForCategory(ResourceLocation categoryId) {
        return switch (categoryId.toString()) {
            case "incore:basic_supplies" -> PlayerFeatureUnlockIds.SHOP_BASIC_SUPPLIES;
            case "incore:field_requisitions" -> PlayerFeatureUnlockIds.SHOP_FIELD_REQUISITIONS;
            case "incore:industrial_components" -> PlayerFeatureUnlockIds.SHOP_INDUSTRIAL_COMPONENTS;
            case "incore:daily_exchange" -> PlayerFeatureUnlockIds.SHOP_DAILY_EXCHANGE;
            case "incore:exchange_coolants" -> PlayerFeatureUnlockIds.SHOP_EXCHANGE_COOLANTS;
            case "incore:chartered_rotation" -> PlayerFeatureUnlockIds.SHOP_CHARTERED_ROTATION;
            case "incore:boutique_premium_gear" -> PlayerFeatureUnlockIds.SHOP_BOUTIQUE_PREMIUM_GEAR;
            case "incore:vendor_daily_deals" -> PlayerFeatureUnlockIds.SHOP_VENDOR_DAILY_DEALS;
            case "incore:archive_artifacts" -> PlayerFeatureUnlockIds.SHOP_ARCHIVE_ARTIFACTS;
            case "incore:expedition_cache" -> PlayerFeatureUnlockIds.SHOP_EXPEDITION_CACHE;
            case "incore:salvage_exchange" -> PlayerFeatureUnlockIds.SHOP_SALVAGE_EXCHANGE;
            case "incore:abyssal_signal_kits" -> PlayerFeatureUnlockIds.SHOP_ABYSSAL_SIGNAL_KITS;
            default -> null;
        };
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
        long now = System.currentTimeMillis();
        for (ShopCategoryDefinition category : ShopCategoryManager.all()) {
            changed |= ensureStockInitialized(playerState, category);
            String token = replenishToken(category, now);
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

    private static String replenishToken(ShopCategoryDefinition category, long now) {
        return switch (category.replenishMode()) {
            case NONE -> "";
            case DAILY_NOON -> "daily:" + MarketTime.noonDayKey(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()));
            case SHOP_ROTATION -> {
                if (category.rotation() == null) {
                    yield "shop:missing:" + category.id();
                }
                yield "shop:" + category.id() + "#" + rotationStep(now, category.rotation());
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
            default -> throw new IllegalArgumentException("Unknown stock mode " + category.stockMode() + " for " + category.id());
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
            default -> throw new IllegalArgumentException("Unknown stock mode " + category.stockMode() + " for " + category.id());
        }
    }

    private static ShopCurrencySpec effectiveCurrencySpec(ShopCategoryDefinition category, ShopOfferDefinition offer) {
        return offer.currencyOverride() != null ? offer.currencyOverride() : category.defaultCurrency();
    }

    private static CurrencyView currencyView(ServerPlayer player, ShopCurrencySpec spec, int amountPerUnit) {
        ShopCurrencyType type = ShopCurrencyRegistry.get(spec.typeId());
        if (type == null) {
            return new CurrencyView("", amountPerUnit, 0);
        }
        ShopCurrencyView built = type.buildView(player, spec, amountPerUnit);
        return new CurrencyView(
                built.label(),
                built.amountPerUnit(),
                built.availableAmount()
        );
    }

    private static List<VisibleOffer> visibleOffersForCategory(long now, ShopCategoryDefinition category) {
        List<ShopOfferDefinition> baseOffers = ShopOfferManager.byCategory(category.id()).stream()
                .sorted(Comparator.comparing(offer -> offer.id().toString()))
                .toList();
        if (baseOffers.isEmpty()) {
            return List.of();
        }
        if (category.rotation() == null) {
            return baseOffers.stream()
                    .map(offer -> new VisibleOffer(offer, -1L, 0))
                    .sorted(visibleOfferComparator(category.offerSortMode()))
                    .toList();
        }

        ShopCategoryRotationDefinition rotation = category.rotation();
        int offerCount = baseOffers.size();
        int visibleCount = Math.min(rotation.visibleCount(), offerCount);
        int start = Math.floorMod((int) rotationStep(now, rotation), offerCount);
        long remaining = remainingMillisForCurrentStep(now, rotation);
        List<VisibleOffer> visible = new ArrayList<>();
        for (int offset = 0; offset < visibleCount; offset++) {
            int index = Math.floorMod(start + offset, offerCount);
            visible.add(new VisibleOffer(
                    baseOffers.get(index),
                    remaining + (long) offset * rotationDurationMillis(rotation),
                    offset
            ));
        }
        visible.sort(visibleOfferComparator(category.offerSortMode()));
        return List.copyOf(visible);
    }

    private static Comparator<VisibleOffer> visibleOfferComparator(ShopOfferSortMode sortMode) {
        return switch (sortMode) {
            case ID -> Comparator.comparing(visibleOffer -> visibleOffer.offer().id().toString());
            case ROTATION_TIME_REMAINING -> Comparator
                    .comparingLong(VisibleOffer::rotationRemainingMillis)
                    .thenComparingInt(VisibleOffer::windowOffset)
                    .thenComparing(visibleOffer -> visibleOffer.offer().id().toString());
        };
    }

    private static boolean isOfferCurrentlyVisible(ShopCategoryDefinition category, ResourceLocation offerId, long now) {
        return visibleOffersForCategory(now, category).stream().anyMatch(offer -> offer.offer().id().equals(offerId));
    }

    private static List<RewardEntryView> rewardEntryViews(ShopPurchaseableDefinition purchaseable) {
        return switch (purchaseable) {
            case ShopSingleItemPurchaseableDefinition singleItem -> List.of(new RewardEntryView(singleItem.stackSpec(), singleItem.count()));
            case ShopBundlePurchaseableDefinition bundle -> bundle.items().stream()
                    .map(entry -> new RewardEntryView(entry.stackSpec(), entry.count()))
                    .toList();
        };
    }

    private static List<ItemStack> resolveGrantedStacks(ShopPurchaseableDefinition purchaseable, int quantity) {
        List<ItemStack> stacks = new ArrayList<>();
        switch (purchaseable) {
            case ShopSingleItemPurchaseableDefinition singleItem -> addGrantedStack(stacks, singleItem.stackSpec(), singleItem.count(), quantity);
            case ShopBundlePurchaseableDefinition bundle -> {
                for (ShopRewardStackDefinition entry : bundle.items()) {
                    addGrantedStack(stacks, entry.stackSpec(), entry.count(), quantity);
                }
            }
        }
        return stacks;
    }

    private static void addGrantedStack(List<ItemStack> stacks, String stackSpec, int countPerUnit, int quantity) {
        long total = (long) countPerUnit * quantity;
        if (total <= 0L || total > Integer.MAX_VALUE) {
            return;
        }
        ItemStack stack = parseStack(stackSpec, (int) total);
        if (stack != null && !stack.isEmpty()) {
            stacks.add(stack);
        }
    }

    private static boolean canFitAll(ServerPlayer player, List<ItemStack> stacks) {
        List<ItemStack> simulated = new ArrayList<>(player.getInventory().items.size());
        for (ItemStack stack : player.getInventory().items) {
            simulated.add(stack.copy());
        }
        for (ItemStack stack : expandStacks(stacks)) {
            if (!insertIntoSimulation(simulated, stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean insertIntoSimulation(List<ItemStack> inventory, ItemStack incoming) {
        for (ItemStack slot : inventory) {
            if (slot.isEmpty() || !ItemStack.isSameItemSameComponents(slot, incoming)) {
                continue;
            }
            int space = slot.getMaxStackSize() - slot.getCount();
            if (space <= 0) {
                continue;
            }
            int moved = Math.min(space, incoming.getCount());
            slot.grow(moved);
            incoming.shrink(moved);
            if (incoming.isEmpty()) {
                return true;
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            if (!inventory.get(i).isEmpty()) {
                continue;
            }
            inventory.set(i, incoming.copy());
            return true;
        }
        return false;
    }

    private static List<ItemStack> expandStacks(List<ItemStack> stacks) {
        List<ItemStack> expanded = new ArrayList<>();
        for (ItemStack stack : stacks) {
            int remaining = stack.getCount();
            while (remaining > 0) {
                int next = Math.min(stack.getMaxStackSize(), remaining);
                expanded.add(stack.copyWithCount(next));
                remaining -= next;
            }
        }
        return expanded;
    }

    private static void giveOrDropAll(ServerPlayer player, List<ItemStack> stacks) {
        for (ItemStack stack : stacks) {
            VendingMachineItemUtil.giveOrDropStacked(player, stack);
        }
    }

    private static long rotationStep(long now, ShopCategoryRotationDefinition rotation) {
        return Math.floorDiv(now, rotationDurationMillis(rotation));
    }

    private static long remainingMillisForCurrentStep(long now, ShopCategoryRotationDefinition rotation) {
        long duration = rotationDurationMillis(rotation);
        long elapsed = Math.floorMod(now, duration);
        return Math.max(0L, duration - elapsed);
    }

    private static long rotationDurationMillis(ShopCategoryRotationDefinition rotation) {
        return Math.max(1L, rotation.durationHours()) * 60L * 60L * 1000L;
    }

    private static long uiRemainingMillis(long remainingMillis) {
        if (remainingMillis < 0L) {
            return -1L;
        }
        if (remainingMillis == 0L) {
            return 0L;
        }
        return ((remainingMillis + UI_TIMER_BUCKET_MILLIS - 1L) / UI_TIMER_BUCKET_MILLIS) * UI_TIMER_BUCKET_MILLIS;
    }

    private static @Nullable TabView findTab(ScreenData data, ShopTabId tabId) {
        for (TabView tab : data.tabs()) {
            if (tab.tabId().equals(tabId.serialized())) {
                return tab;
            }
        }
        return null;
    }

    private static @Nullable CategoryView findCategory(ScreenData data, String categoryId) {
        for (CategoryView category : data.categories()) {
            if (category.categoryId().equals(categoryId)) {
                return category;
            }
        }
        return null;
    }

    private static List<OfferView> offersForCategory(ScreenData data, String categoryId) {
        return data.offers().stream()
                .filter(offer -> offer.categoryId().equals(categoryId))
                .toList();
    }

    private static List<OfferView> selectShowcaseOffers(
            ScreenData data,
            TabView tab,
            List<CategoryView> orderedCategories,
            String activeCategoryId,
            List<OfferView> primaryFeed
    ) {
        if (!tab.showcase().enabled() || tab.showcase().slots() <= 0) {
            return List.of();
        }

        List<OfferView> showcaseCandidates = switch (ShopShowcaseSource.fromString(tab.showcase().source())) {
            case TOP_OF_FEED -> primaryFeed;
            case ROTATING_FIRST -> rotatingFirstCandidates(data, tab, orderedCategories, primaryFeed);
            case CATEGORY_PINNED -> categoryPinnedCandidates(data, tab, orderedCategories, activeCategoryId);
        };

        if (showcaseCandidates.isEmpty()) {
            return List.of();
        }
        int end = Math.min(tab.showcase().slots(), showcaseCandidates.size());
        return List.copyOf(showcaseCandidates.subList(0, end));
    }

    private static List<OfferView> rotatingFirstCandidates(
            ScreenData data,
            TabView tab,
            List<CategoryView> orderedCategories,
            List<OfferView> primaryFeed
    ) {
        List<String> scope = !tab.showcase().categoryScope().isEmpty()
                ? tab.showcase().categoryScope()
                : orderedCategories.stream().map(CategoryView::categoryId).toList();
        for (String categoryId : scope) {
            CategoryView category = findCategory(data, categoryId);
            if (category == null || !category.rotating()) {
                continue;
            }
            List<OfferView> categoryOffers = offersForCategory(data, categoryId);
            if (!categoryOffers.isEmpty()) {
                return categoryOffers;
            }
        }
        return primaryFeed;
    }

    private static List<OfferView> categoryPinnedCandidates(
            ScreenData data,
            TabView tab,
            List<CategoryView> orderedCategories,
            String activeCategoryId
    ) {
        List<String> scope = !tab.showcase().categoryScope().isEmpty()
                ? tab.showcase().categoryScope()
                : orderedCategories.stream().map(CategoryView::categoryId).toList();
        for (String categoryId : scope) {
            List<OfferView> categoryOffers = offersForCategory(data, categoryId);
            if (!categoryOffers.isEmpty()) {
                return categoryOffers;
            }
        }
        return offersForCategory(data, activeCategoryId);
    }

    public record ScreenData(
            String selectedCategoryId,
            String selectedOfferId,
            List<TabView> tabs,
            List<CategoryView> categories,
            List<OfferView> offers
    ) {
    }

    public record TabView(
            String tabId,
            String displayName,
            String paletteId,
            String layoutId,
            String categoryNavigation,
            String detailsMode,
            List<String> categoryIds,
            ShowcaseView showcase
    ) {
    }

    public record ShowcaseView(
            boolean enabled,
            int slots,
            String source,
            List<String> categoryScope
    ) {
    }

    public record CategoryView(
            String categoryId,
            String displayName,
            String stockMode,
            String replenishMode,
            int availableStock,
            boolean locked,
            CurrencyView currency,
            boolean rotating,
            long rotationRemainingMillis,
            int visibleOfferCount
    ) {
    }

    public record OfferView(
            String offerId,
            String categoryId,
            String displayName,
            int price,
            CurrencyView currency,
            List<RewardEntryView> rewardEntries,
            int availableStock,
            boolean locked,
            long rotationRemainingMillis
    ) {
    }

    public record CurrencyView(
            String label,
            int amountPerUnit,
            int availableAmount
    ) {
    }

    public record RewardEntryView(String stackSpec, int count) {
    }

    public record TabFeedView(
            String activeCategoryId,
            List<String> orderedCategoryIds,
            List<OfferView> showcaseOffers,
            List<OfferView> remainingOffers
    ) {
    }

    record RequestedSelection(
            @Nullable ResourceLocation categoryId,
            @Nullable ResourceLocation offerId
    ) {
    }

    private record VisibleOffer(ShopOfferDefinition offer, long rotationRemainingMillis, int windowOffset) {
    }
}
