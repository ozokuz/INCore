package io.github.ozokuz.incore.features.vendingmachine;

import com.google.gson.Gson;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.tasks.DailyTaskEvents;
import io.github.ozokuz.incore.features.battlepass.BattlePassTaskHooks;
import io.github.ozokuz.incore.features.vendingmachine.network.VendingMachineNetworking;
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

public final class VendingMachineService {
    private static final Gson GSON = new Gson();
    private static final int OFFERS_PER_VENDING_MACHINE = 5;
    private static final int DARK_MARKET_CHANCE_PERCENT = 5;
    private static final double MAX_VENDING_MACHINE_DISTANCE_SQR = 64.0D;

    private VendingMachineService() {
    }

    public static void openVendingMachineScreen(ServerPlayer player, BlockPos vending_machinePos) {
        VendingMachineBlockEntity vending_machine = getVendingMachineBlockEntity(player, vending_machinePos);
        if (vending_machine == null) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.invalid_vending_machine"));
            return;
        }

        ensureInventoryInitialized(player, vending_machine);
        VendingMachineScreenData data = buildScreenData(player, vending_machinePos, vending_machine);
        VendingMachineNetworking.openVendingMachineScreen(player, GSON.toJson(data));
    }

    public static boolean purchase(
            ServerPlayer player,
            BlockPos vending_machinePos,
            ResourceLocation offerId,
            int quantity,
            boolean allowConversion
    ) {
        if (quantity <= 0) {
            return false;
        }

        VendingMachineBlockEntity vending_machine = getVendingMachineBlockEntity(player, vending_machinePos);
        if (vending_machine == null) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.invalid_vending_machine"));
            return false;
        }

        if (!isWithinVendingMachineRange(player, vending_machinePos)) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.too_far"));
            return false;
        }

        ensureInventoryInitialized(player, vending_machine);

        if (!vending_machine.hasOffer(offerId)) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.invalid_offer", offerId.toString()));
            return false;
        }

        VendingMachineOfferData offer = VendingMachineOfferManager.get(offerId);
        if (offer == null) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.invalid_offer", offerId.toString()));
            return false;
        }

        if (!offer.productType().isAvailable(offer.productSpec())) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.invalid_offer", offerId.toString()));
            return false;
        }

        int stockRemaining = vending_machine.stockFor(offerId);
        if (stockRemaining < quantity) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.out_of_stock", quantity, stockRemaining));
            return false;
        }

        if (!vending_machine.consumeStock(offerId, quantity)) {
            player.sendSystemMessage(Component.translatable("incore.vending_machine.out_of_stock", quantity, vending_machine.stockFor(offerId)));
            return false;
        }

        VendingMachineCurrencySpec pricedCurrencySpec = pricedCurrencySpec(player, vending_machine, offer);
        if (!offer.currencyType().consume(player, pricedCurrencySpec, quantity, allowConversion)) {
            vending_machine.addStock(offerId, quantity);
            player.sendSystemMessage(Component.translatable("incore.vending_machine.not_enough_currency"));
            return false;
        }

        if (!offer.productType().grant(player, offer.productSpec(), quantity)) {
            vending_machine.addStock(offerId, quantity);
            player.sendSystemMessage(Component.translatable("incore.vending_machine.grant_failed"));
            return false;
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable(
                "incore.vending_machine.purchased",
                offer.name(),
                Math.max(1, offer.productSpec().unitCount()) * quantity
        ));
        DailyTaskEvents.onVendingMachinePurchase(player);
        BattlePassTaskHooks.onVendingMachinePurchase(player);
        return true;
    }

    private static VendingMachineScreenData buildScreenData(ServerPlayer player, BlockPos vending_machinePos, VendingMachineBlockEntity vending_machine) {
        List<VendingMachineOfferView> offers = new ArrayList<>();
        for (var entry : vending_machine.offerStocks().entrySet()) {
            VendingMachineOfferData offer = VendingMachineOfferManager.get(entry.getKey());
            if (offer == null || !offer.productType().isAvailable(offer.productSpec())) {
                continue;
            }

            ItemStack preview = offer.productType().previewStack(offer.productSpec());
            ResourceLocation previewItemId = preview.isEmpty() ? BuiltInRegistries.ITEM.getKey(Items.BARRIER) : BuiltInRegistries.ITEM.getKey(preview.getItem());
            int baseAmountPerUnit = Math.max(1, offer.currencySpec().unitAmount());
            DiscountResolution discount = resolveDiscount(player, vending_machine, offer);
            int discountPercent = discount.percent();
            boolean curioOnlyDiscount = discount.curioOnly();
            VendingMachineCurrencySpec pricedCurrencySpec = pricedCurrencySpec(offer, discountPercent);
            int effectiveAmountPerUnit = Math.max(1, pricedCurrencySpec.unitAmount());
            if (effectiveAmountPerUnit >= baseAmountPerUnit) {
                discountPercent = 0;
                curioOnlyDiscount = false;
            }

            offers.add(new VendingMachineOfferView(
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
        return new VendingMachineScreenData(
                offers,
                balanceEntries,
                vending_machinePos.asLong(),
                vending_machine.darkMarket(),
                vending_machine.categoryId() == null ? null : vending_machine.categoryId().toString()
        );
    }

    private static List<BalanceEntryView> collectBalanceEntries(List<VendingMachineOfferView> offers) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<BalanceEntryView> entries = new ArrayList<>();

        for (VendingMachineOfferView offer : offers) {
            appendCurrencyBalanceEntries(entries, seen, offer.currency());
        }

        return entries;
    }

    public static List<BalanceEntryView> collectPlayerBalanceEntries(ServerPlayer player) {
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        List<BalanceEntryView> entries = new ArrayList<>();

        List<VendingMachineOfferData> offers = VendingMachineOfferManager.all().stream()
                .filter(offer -> offer.productType().isAvailable(offer.productSpec()))
                .sorted(Comparator.comparing(offer -> offer.id().toString()))
                .toList();
        for (VendingMachineOfferData offer : offers) {
            VendingMachineCurrencyView currency = offer.currencyType().buildView(player, offer.currencySpec());
            appendCurrencyBalanceEntries(entries, seen, currency);
        }

        return entries;
    }

    private static void appendCurrencyBalanceEntries(
            List<BalanceEntryView> entries,
            LinkedHashSet<String> seen,
            VendingMachineCurrencyView currency
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

    private static void ensureInventoryInitialized(ServerPlayer player, VendingMachineBlockEntity vending_machine) {
        if (vending_machine.initialized()) {
            backfillMissingDiscounts(player, vending_machine);
            return;
        }

        List<VendingMachineOfferData> allOffers = VendingMachineOfferManager.all().stream()
                .filter(offer -> offer.productType().isAvailable(offer.productSpec()))
                .sorted(Comparator.comparingInt(VendingMachineOfferData::weight).reversed().thenComparing(offer -> offer.id().toString()))
                .toList();
        if (allOffers.isEmpty()) {
            vending_machine.initializeInventory(false, null, new LinkedHashMap<>(), new LinkedHashMap<>());
            return;
        }

        RandomSource random = player.getRandom();
        boolean darkMarket = random.nextInt(100) < DARK_MARKET_CHANCE_PERCENT;
        @Nullable ResourceLocation selectedCategory = null;

        List<VendingMachineOfferData> pool = allOffers;
        if (!darkMarket) {
            List<ResourceLocation> categories = allOffers.stream()
                    .map(VendingMachineOfferData::category)
                    .distinct()
                    .toList();
            if (!categories.isEmpty()) {
                selectedCategory = categories.get(random.nextInt(categories.size()));
                ResourceLocation categoryForFilter = selectedCategory;
                List<VendingMachineOfferData> themedPool = allOffers.stream()
                        .filter(offer -> offer.category().equals(categoryForFilter))
                        .toList();
                if (!themedPool.isEmpty()) {
                    pool = themedPool;
                } else {
                    selectedCategory = null;
                }
            }
        }

        List<VendingMachineOfferData> remaining = new ArrayList<>(pool);
        LinkedHashMap<ResourceLocation, Integer> rolledInventory = new LinkedHashMap<>();
        LinkedHashMap<ResourceLocation, Integer> rolledDiscounts = new LinkedHashMap<>();
        int selectionCount = Math.min(OFFERS_PER_VENDING_MACHINE, remaining.size());

        for (int i = 0; i < selectionCount; i++) {
            int pickedIndex = rollWeightedIndex(remaining, random);
            if (pickedIndex < 0) {
                break;
            }

            VendingMachineOfferData selected = remaining.remove(pickedIndex);
            int stock = selected.stockMin();
            if (selected.stockMax() > selected.stockMin()) {
                stock += random.nextInt(selected.stockMax() - selected.stockMin() + 1);
            }
            rolledInventory.put(selected.id(), stock);
            if (darkMarket) {
                rolledDiscounts.put(selected.id(), VendingMachineDiscountService.rollDarkMarketOfferDiscountPercent(random));
            } else {
                rolledDiscounts.put(selected.id(), VendingMachineDiscountService.rollNormalOfferDiscountPercent(random));
            }
        }

        vending_machine.initializeInventory(darkMarket, selectedCategory, rolledInventory, rolledDiscounts);
    }

    private static void backfillMissingDiscounts(ServerPlayer player, VendingMachineBlockEntity vending_machine) {
        Map<ResourceLocation, Integer> stocks = vending_machine.offerStocks();
        if (stocks.isEmpty()) {
            return;
        }

        RandomSource random = player.getRandom();
        for (ResourceLocation offerId : stocks.keySet()) {
            if (vending_machine.hasDiscountEntry(offerId)) {
                continue;
            }

            VendingMachineOfferData offer = VendingMachineOfferManager.get(offerId);
            if (offer == null) {
                vending_machine.setOfferDiscount(offerId, 0);
                continue;
            }

            int discount = vending_machine.darkMarket()
                    ? VendingMachineDiscountService.rollDarkMarketOfferDiscountPercent(random)
                    : VendingMachineDiscountService.rollNormalOfferDiscountPercent(random);
            vending_machine.setOfferDiscount(offerId, discount);
        }
    }

    private static VendingMachineCurrencySpec pricedCurrencySpec(ServerPlayer player, VendingMachineBlockEntity vending_machine, VendingMachineOfferData offer) {
        return pricedCurrencySpec(offer, resolveDiscount(player, vending_machine, offer).percent());
    }

    private static VendingMachineCurrencySpec pricedCurrencySpec(VendingMachineOfferData offer, int discountPercent) {
        VendingMachineCurrencySpec baseSpec = offer.currencySpec();
        int baseAmount = Math.max(1, baseSpec.unitAmount());
        if (discountPercent <= 0) {
            return baseSpec;
        }

        int discountedAmount = VendingMachineDiscountService.applyDiscountedUnitAmount(baseAmount, discountPercent);
        if (discountedAmount >= baseAmount) {
            return baseSpec;
        }

        return offer.currencyType().withUnitAmount(baseSpec, discountedAmount);
    }

    private static DiscountResolution resolveDiscount(ServerPlayer player, VendingMachineBlockEntity vending_machine, VendingMachineOfferData offer) {
        int baseDiscount = Math.max(0, vending_machine.discountPercentFor(offer.id()));
        if (!VendingMachineDiscountService.hasDiscountCharmEquipped(player)) {
            return new DiscountResolution(baseDiscount, false);
        }

        int bonusAmount = VendingMachineDiscountService.curioBonusAmountPercent();
        if (baseDiscount > 0) {
            return new DiscountResolution(Math.clamp(baseDiscount + bonusAmount, 0, 100), false);
        }

        int curioOnlyDiscount = temporaryCurioOnlyDiscountPercent(vending_machine, offer.id(), bonusAmount);
        return new DiscountResolution(curioOnlyDiscount, curioOnlyDiscount > 0);
    }

    private static int temporaryCurioOnlyDiscountPercent(
            VendingMachineBlockEntity vending_machine,
            ResourceLocation offerId,
            int bonusAmountPercent
    ) {
        int bonusChance = VendingMachineDiscountService.curioBonusChancePercent();
        if (bonusChance <= 0) {
            return 0;
        }

        RandomSource deterministicRoll = RandomSource.create(stableOfferSeed(vending_machine, offerId, 0x74A5A2D4L));
        if (deterministicRoll.nextInt(100) >= bonusChance) {
            return 0;
        }

        int min = Math.clamp(VendingMachineDiscountService.baseDiscountMinPercent() + bonusAmountPercent, 0, 100);
        int max = Math.clamp(VendingMachineDiscountService.baseDiscountMaxPercent() + bonusAmountPercent, 0, 100);
        if (max < min) {
            min = max;
        }
        if (max <= 0) {
            return 0;
        }

        RandomSource deterministicAmount = RandomSource.create(stableOfferSeed(vending_machine, offerId, 0x5D32B11EL));
        return min + deterministicAmount.nextInt(max - min + 1);
    }

    private static long stableOfferSeed(VendingMachineBlockEntity vending_machine, ResourceLocation offerId, long salt) {
        long seed = vending_machine.getBlockPos().asLong();
        seed ^= ((long) offerId.hashCode() * 0x9E3779B97F4A7C15L);
        seed ^= salt;
        return seed;
    }

    private static int rollWeightedIndex(List<VendingMachineOfferData> offers, RandomSource random) {
        if (offers.isEmpty()) {
            return -1;
        }

        int totalWeight = 0;
        for (VendingMachineOfferData offer : offers) {
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

    private static VendingMachineBlockEntity getVendingMachineBlockEntity(ServerPlayer player, BlockPos pos) {
        if (!player.level().isLoaded(pos)) {
            return null;
        }

        if (player.level().getBlockState(pos).getBlock() != Registration.VENDING_MACHINE_BLOCK.get()) {
            return null;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        return blockEntity instanceof VendingMachineBlockEntity vending_machine ? vending_machine : null;
    }

    private static boolean isWithinVendingMachineRange(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= MAX_VENDING_MACHINE_DISTANCE_SQR;
    }

    public record VendingMachineScreenData(
            List<VendingMachineOfferView> offers,
            List<BalanceEntryView> balances,
            long vending_machinePosLong,
            boolean darkMarket,
            @Nullable String categoryId
    ) {
    }

    public record VendingMachineOfferView(
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
            VendingMachineCurrencyView currency,
            int stockRemaining
    ) {
    }

    public record BalanceEntryView(String iconItemId, int amount) {
    }

    private record DiscountResolution(int percent, boolean curioOnly) {
    }
}
