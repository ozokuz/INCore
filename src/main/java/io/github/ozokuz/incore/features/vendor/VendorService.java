package io.github.ozokuz.incore.features.vendor;

import com.google.gson.Gson;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.tasks.DailyTaskEvents;
import io.github.ozokuz.incore.features.vendor.network.VendorNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class VendorService {
    private static final Gson GSON = new Gson();
    private static final int OFFERS_PER_VENDOR = 5;
    private static final int DARK_MARKET_CHANCE_PERCENT = 5;
    private static final double MAX_VENDOR_DISTANCE_SQR = 64.0D;

    private VendorService() {
    }

    public static void openVendorScreen(ServerPlayer player, BlockPos vendorPos) {
        VendorBlockEntity vendor = getVendorBlockEntity(player, vendorPos);
        if (vendor == null) {
            player.sendSystemMessage(Component.translatable("incore.vendor.invalid_vendor"));
            return;
        }

        ensureInventoryInitialized(player, vendor);
        VendorScreenData data = buildScreenData(player, vendorPos, vendor);
        VendorNetworking.openVendorScreen(player, GSON.toJson(data));
    }

    public static boolean purchase(
            ServerPlayer player,
            BlockPos vendorPos,
            ResourceLocation offerId,
            int quantity,
            boolean allowConversion
    ) {
        if (quantity <= 0) {
            return false;
        }

        VendorBlockEntity vendor = getVendorBlockEntity(player, vendorPos);
        if (vendor == null) {
            player.sendSystemMessage(Component.translatable("incore.vendor.invalid_vendor"));
            return false;
        }

        if (!isWithinVendorRange(player, vendorPos)) {
            player.sendSystemMessage(Component.translatable("incore.vendor.too_far"));
            return false;
        }

        ensureInventoryInitialized(player, vendor);

        if (!vendor.hasOffer(offerId)) {
            player.sendSystemMessage(Component.translatable("incore.vendor.invalid_offer", offerId.toString()));
            return false;
        }

        VendorOfferData offer = VendorOfferManager.get(offerId);
        if (offer == null) {
            player.sendSystemMessage(Component.translatable("incore.vendor.invalid_offer", offerId.toString()));
            return false;
        }

        if (!offer.productType().isAvailable(offer.productSpec())) {
            player.sendSystemMessage(Component.translatable("incore.vendor.invalid_offer", offerId.toString()));
            return false;
        }

        int stockRemaining = vendor.stockFor(offerId);
        if (stockRemaining < quantity) {
            player.sendSystemMessage(Component.translatable("incore.vendor.out_of_stock", quantity, stockRemaining));
            return false;
        }

        if (!vendor.consumeStock(offerId, quantity)) {
            player.sendSystemMessage(Component.translatable("incore.vendor.out_of_stock", quantity, vendor.stockFor(offerId)));
            return false;
        }

        VendorCurrencySpec pricedCurrencySpec = pricedCurrencySpec(player, vendor, offer);
        if (!offer.currencyType().consume(player, pricedCurrencySpec, quantity, allowConversion)) {
            vendor.addStock(offerId, quantity);
            player.sendSystemMessage(Component.translatable("incore.vendor.not_enough_currency"));
            return false;
        }

        if (!offer.productType().grant(player, offer.productSpec(), quantity)) {
            vendor.addStock(offerId, quantity);
            player.sendSystemMessage(Component.translatable("incore.vendor.grant_failed"));
            return false;
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable(
                "incore.vendor.purchased",
                offer.name(),
                Math.max(1, offer.productSpec().unitCount()) * quantity
        ));
        DailyTaskEvents.onVendorPurchase(player);
        return true;
    }

    private static VendorScreenData buildScreenData(ServerPlayer player, BlockPos vendorPos, VendorBlockEntity vendor) {
        List<VendorOfferView> offers = new ArrayList<>();
        for (var entry : vendor.offerStocks().entrySet()) {
            VendorOfferData offer = VendorOfferManager.get(entry.getKey());
            if (offer == null || !offer.productType().isAvailable(offer.productSpec())) {
                continue;
            }

            ItemStack preview = offer.productType().previewStack(offer.productSpec());
            ResourceLocation previewItemId = preview.isEmpty() ? BuiltInRegistries.ITEM.getKey(Items.BARRIER) : BuiltInRegistries.ITEM.getKey(preview.getItem());
            int baseAmountPerUnit = Math.max(1, offer.currencySpec().unitAmount());
            DiscountResolution discount = resolveDiscount(player, vendor, offer);
            int discountPercent = discount.percent();
            boolean curioOnlyDiscount = discount.curioOnly();
            VendorCurrencySpec pricedCurrencySpec = pricedCurrencySpec(offer, discountPercent);
            int effectiveAmountPerUnit = Math.max(1, pricedCurrencySpec.unitAmount());
            if (effectiveAmountPerUnit >= baseAmountPerUnit) {
                discountPercent = 0;
                curioOnlyDiscount = false;
            }

            offers.add(new VendorOfferView(
                    offer.id().toString(),
                    offer.name(),
                    offer.productType().id().toString(),
                    offer.productType().productId(offer.productSpec()),
                    Math.max(1, offer.productSpec().unitCount()),
                    previewItemId.toString(),
                    baseAmountPerUnit,
                    effectiveAmountPerUnit,
                    discountPercent,
                    curioOnlyDiscount,
                    offer.currencyType().buildView(player, pricedCurrencySpec),
                    Math.max(0, entry.getValue())
            ));
        }

        List<BalanceEntryView> balanceEntries = collectBalanceEntries(offers);
        return new VendorScreenData(
                offers,
                balanceEntries,
                vendorPos.asLong(),
                vendor.darkMarket(),
                vendor.categoryId() == null ? null : vendor.categoryId().toString()
        );
    }

    private static List<BalanceEntryView> collectBalanceEntries(List<VendorOfferView> offers) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<BalanceEntryView> entries = new ArrayList<>();

        for (VendorOfferView offer : offers) {
            appendCurrencyBalanceEntries(entries, seen, offer.currency());
        }

        return entries;
    }

    public static List<BalanceEntryView> collectPlayerBalanceEntries(ServerPlayer player) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<BalanceEntryView> entries = new ArrayList<>();

        List<VendorOfferData> offers = VendorOfferManager.all().stream()
                .filter(offer -> offer.productType().isAvailable(offer.productSpec()))
                .sorted(Comparator.comparing(offer -> offer.id().toString()))
                .toList();
        for (VendorOfferData offer : offers) {
            VendorCurrencyView currency = offer.currencyType().buildView(player, offer.currencySpec());
            appendCurrencyBalanceEntries(entries, seen, currency);
        }

        return entries;
    }

    private static void appendCurrencyBalanceEntries(
            List<BalanceEntryView> entries,
            LinkedHashSet<String> seen,
            VendorCurrencyView currency
    ) {
        if (currency.primaryIconItemId() != null
                && !currency.primaryIconItemId().isBlank()
                && !seen.contains(currency.primaryIconItemId())) {
            seen.add(currency.primaryIconItemId());
            entries.add(new BalanceEntryView(currency.primaryIconItemId(), Math.max(0, currency.availablePrimary())));
        }
        if (currency.conversionIconItemId() != null
                && !currency.conversionIconItemId().isBlank()
                && !seen.contains(currency.conversionIconItemId())) {
            seen.add(currency.conversionIconItemId());
            entries.add(new BalanceEntryView(currency.conversionIconItemId(), Math.max(0, currency.availableConversion())));
        }
    }

    private static void ensureInventoryInitialized(ServerPlayer player, VendorBlockEntity vendor) {
        if (vendor.initialized()) {
            backfillMissingDiscounts(player, vendor);
            return;
        }

        List<VendorOfferData> allOffers = VendorOfferManager.all().stream()
                .filter(offer -> offer.productType().isAvailable(offer.productSpec()))
                .sorted(Comparator.comparingInt(VendorOfferData::weight).reversed().thenComparing(offer -> offer.id().toString()))
                .toList();
        if (allOffers.isEmpty()) {
            vendor.initializeInventory(false, null, new LinkedHashMap<>(), new LinkedHashMap<>());
            return;
        }

        RandomSource random = player.getRandom();
        boolean darkMarket = random.nextInt(100) < DARK_MARKET_CHANCE_PERCENT;
        @Nullable ResourceLocation selectedCategory = null;

        List<VendorOfferData> pool = allOffers;
        if (!darkMarket) {
            List<ResourceLocation> categories = allOffers.stream()
                    .map(VendorOfferData::category)
                    .distinct()
                    .toList();
            if (!categories.isEmpty()) {
                selectedCategory = categories.get(random.nextInt(categories.size()));
                ResourceLocation categoryForFilter = selectedCategory;
                List<VendorOfferData> themedPool = allOffers.stream()
                        .filter(offer -> offer.category().equals(categoryForFilter))
                        .toList();
                if (!themedPool.isEmpty()) {
                    pool = themedPool;
                } else {
                    selectedCategory = null;
                }
            }
        }

        List<VendorOfferData> remaining = new ArrayList<>(pool);
        LinkedHashMap<ResourceLocation, Integer> rolledInventory = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, Integer> rolledDiscounts = new LinkedHashMap<>();
        int selectionCount = Math.min(OFFERS_PER_VENDOR, remaining.size());

        for (int i = 0; i < selectionCount; i++) {
            int pickedIndex = rollWeightedIndex(remaining, random);
            if (pickedIndex < 0) {
                break;
            }

            VendorOfferData selected = remaining.remove(pickedIndex);
            int stock = selected.stockMin();
            if (selected.stockMax() > selected.stockMin()) {
                stock += random.nextInt(selected.stockMax() - selected.stockMin() + 1);
            }
            rolledInventory.put(selected.id(), stock);
            if (darkMarket) {
                rolledDiscounts.put(selected.id(), VendorDiscountService.rollDarkMarketOfferDiscountPercent(random));
            } else {
                rolledDiscounts.put(selected.id(), VendorDiscountService.rollNormalOfferDiscountPercent(random));
            }
        }

        vendor.initializeInventory(darkMarket, selectedCategory, rolledInventory, rolledDiscounts);
    }

    private static void backfillMissingDiscounts(ServerPlayer player, VendorBlockEntity vendor) {
        Map<ResourceLocation, Integer> stocks = vendor.offerStocks();
        if (stocks.isEmpty()) {
            return;
        }

        RandomSource random = player.getRandom();
        for (ResourceLocation offerId : stocks.keySet()) {
            if (vendor.hasDiscountEntry(offerId)) {
                continue;
            }

            VendorOfferData offer = VendorOfferManager.get(offerId);
            if (offer == null) {
                vendor.setOfferDiscount(offerId, 0);
                continue;
            }

            int discount = vendor.darkMarket()
                    ? VendorDiscountService.rollDarkMarketOfferDiscountPercent(random)
                    : VendorDiscountService.rollNormalOfferDiscountPercent(random);
            vendor.setOfferDiscount(offerId, discount);
        }
    }

    private static VendorCurrencySpec pricedCurrencySpec(ServerPlayer player, VendorBlockEntity vendor, VendorOfferData offer) {
        return pricedCurrencySpec(offer, resolveDiscount(player, vendor, offer).percent());
    }

    private static VendorCurrencySpec pricedCurrencySpec(VendorOfferData offer, int discountPercent) {
        VendorCurrencySpec baseSpec = offer.currencySpec();
        int baseAmount = Math.max(1, baseSpec.unitAmount());
        if (discountPercent <= 0) {
            return baseSpec;
        }

        int discountedAmount = VendorDiscountService.applyDiscountedUnitAmount(baseAmount, discountPercent);
        if (discountedAmount >= baseAmount) {
            return baseSpec;
        }

        return offer.currencyType().withUnitAmount(baseSpec, discountedAmount);
    }

    private static DiscountResolution resolveDiscount(ServerPlayer player, VendorBlockEntity vendor, VendorOfferData offer) {
        int baseDiscount = Math.max(0, vendor.discountPercentFor(offer.id()));
        if (!VendorDiscountService.hasDiscountCharmEquipped(player)) {
            return new DiscountResolution(baseDiscount, false);
        }

        int bonusAmount = VendorDiscountService.curioBonusAmountPercent();
        if (baseDiscount > 0) {
            return new DiscountResolution(Math.clamp(baseDiscount + bonusAmount, 0, 100), false);
        }

        int curioOnlyDiscount = temporaryCurioOnlyDiscountPercent(vendor, offer.id(), bonusAmount);
        return new DiscountResolution(curioOnlyDiscount, curioOnlyDiscount > 0);
    }

    private static int temporaryCurioOnlyDiscountPercent(
            VendorBlockEntity vendor,
            ResourceLocation offerId,
            int bonusAmountPercent
    ) {
        int bonusChance = VendorDiscountService.curioBonusChancePercent();
        if (bonusChance <= 0) {
            return 0;
        }

        RandomSource deterministicRoll = RandomSource.create(stableOfferSeed(vendor, offerId, 0x74A5A2D4L));
        if (deterministicRoll.nextInt(100) >= bonusChance) {
            return 0;
        }

        int min = Math.clamp(VendorDiscountService.baseDiscountMinPercent() + bonusAmountPercent, 0, 100);
        int max = Math.clamp(VendorDiscountService.baseDiscountMaxPercent() + bonusAmountPercent, 0, 100);
        if (max < min) {
            min = max;
        }
        if (max <= 0) {
            return 0;
        }

        RandomSource deterministicAmount = RandomSource.create(stableOfferSeed(vendor, offerId, 0x5D32B11EL));
        return min + deterministicAmount.nextInt(max - min + 1);
    }

    private static long stableOfferSeed(VendorBlockEntity vendor, ResourceLocation offerId, long salt) {
        long seed = vendor.getBlockPos().asLong();
        seed ^= ((long) offerId.hashCode() * 0x9E3779B97F4A7C15L);
        seed ^= salt;
        return seed;
    }

    private static int rollWeightedIndex(List<VendorOfferData> offers, RandomSource random) {
        if (offers.isEmpty()) {
            return -1;
        }

        int totalWeight = 0;
        for (VendorOfferData offer : offers) {
            totalWeight += Math.max(1, offer.weight());
        }
        if (totalWeight <= 0) {
            return -1;
        }

        int value = random.nextInt(totalWeight);
        for (int i = 0; i < offers.size(); i++) {
            value -= Math.max(1, offers.get(i).weight());
            if (value < 0) {
                return i;
            }
        }
        return offers.size() - 1;
    }

    private static VendorBlockEntity getVendorBlockEntity(ServerPlayer player, BlockPos pos) {
        if (!player.level().isLoaded(pos)) {
            return null;
        }

        if (player.level().getBlockState(pos).getBlock() != Registration.VENDOR_BLOCK.get()) {
            return null;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        return blockEntity instanceof VendorBlockEntity vendor ? vendor : null;
    }

    private static boolean isWithinVendorRange(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= MAX_VENDOR_DISTANCE_SQR;
    }

    public record VendorScreenData(
            List<VendorOfferView> offers,
            List<BalanceEntryView> balances,
            long vendorPosLong,
            boolean darkMarket,
            @Nullable String categoryId
    ) {
    }

    public record VendorOfferView(
            String id,
            String name,
            String productType,
            String productId,
            int count,
            String previewItemId,
            int baseAmountPerUnit,
            int effectiveAmountPerUnit,
            int discountPercent,
            boolean curioOnlyDiscount,
            VendorCurrencyView currency,
            int stockRemaining
    ) {
    }

    public record BalanceEntryView(String iconItemId, int amount) {
    }

    private record DiscountResolution(int percent, boolean curioOnly) {
    }
}
