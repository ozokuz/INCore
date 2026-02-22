package io.github.ozokuz.incore.features.cards;

import com.google.gson.Gson;
import dev.ithundxr.createnumismatics.Numismatics;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.cards.network.CardNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class CardVendorService {
    private static final Gson GSON = new Gson();
    public static final int SPUR_PER_TOKEN = 8;
    private static final int OFFERS_PER_VENDOR = 5;
    private static final double MAX_VENDOR_DISTANCE_SQR = 64.0D;

    private CardVendorService() {
    }

    public static void openVendorScreen(ServerPlayer player) {
        if (!openNearestVendorScreen(player, 8)) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.no_nearby_vendor"));
        }
    }

    public static boolean openNearestVendorScreen(ServerPlayer player, int radius) {
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockPos nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (int x = origin.getX() - radius; x <= origin.getX() + radius; x++) {
            for (int y = origin.getY() - radius; y <= origin.getY() + radius; y++) {
                for (int z = origin.getZ() - radius; z <= origin.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    if (player.level().getBlockState(cursor).getBlock() != Registration.CARD_VENDOR_BLOCK.get()) {
                        continue;
                    }

                    double distance = player.distanceToSqr(x + 0.5D, y + 0.5D, z + 0.5D);
                    if (distance < nearestDist) {
                        nearestDist = distance;
                        nearest = cursor.immutable();
                    }
                }
            }
        }

        if (nearest == null) {
            return false;
        }

        openVendorScreen(player, nearest);
        return true;
    }

    public static void openVendorScreen(ServerPlayer player, BlockPos vendorPos) {
        CardVendorBlockEntity vendor = getVendorBlockEntity(player, vendorPos);
        if (vendor == null) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.invalid_vendor"));
            return;
        }

        ensureInventoryInitialized(player, vendor);

        int spurCount = getBankSpurBalance(player);
        int tokenCount = countMatching(player, stack -> stack.getItem() == Registration.CARD_TOKEN_ITEM.get());

        List<VendorOfferView> offers = new ArrayList<>();
        for (var entry : vendor.offerStocks().entrySet()) {
            CardVendorOfferData offer = CardVendorOfferManager.get(entry.getKey());
            if (offer == null) {
                continue;
            }

            offers.add(new VendorOfferView(
                    offer.id().toString(),
                    offer.name(),
                    offer.productType().name().toLowerCase(),
                    offer.productId().toString(),
                    offer.count(),
                    offer.tokenCost(),
                    Math.max(0, entry.getValue())
            ));
        }

        VendorScreenData data = new VendorScreenData(
                offers,
                tokenCount,
                spurCount,
                vendorPos.asLong()
        );

        CardNetworking.openVendorScreen(player, GSON.toJson(data));
    }

    public static boolean purchase(
            ServerPlayer player,
            BlockPos vendorPos,
            ResourceLocation offerId,
            int quantity,
            boolean allowSpurConversion
    ) {
        if (quantity <= 0) {
            return false;
        }

        CardVendorBlockEntity vendor = getVendorBlockEntity(player, vendorPos);
        if (vendor == null) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.invalid_vendor"));
            return false;
        }

        if (!isWithinVendorRange(player, vendorPos)) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.too_far"));
            return false;
        }

        ensureInventoryInitialized(player, vendor);

        if (!vendor.hasOffer(offerId)) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.invalid_offer", offerId.toString()));
            return false;
        }

        CardVendorOfferData offer = CardVendorOfferManager.get(offerId);
        if (offer == null) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.invalid_offer", offerId.toString()));
            return false;
        }

        int stockRemaining = vendor.stockFor(offerId);
        if (stockRemaining < quantity) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.out_of_stock", quantity, stockRemaining));
            return false;
        }

        long rawTokenCost = (long) offer.tokenCost() * quantity;
        long rawRewardCount = (long) offer.count() * quantity;
        if (rawTokenCost > Integer.MAX_VALUE || rawRewardCount > Integer.MAX_VALUE) {
            return false;
        }

        int tokenCost = (int) rawTokenCost;
        int rewardCount = (int) rawRewardCount;

        int tokenCount = countMatching(player, stack -> stack.getItem() == Registration.CARD_TOKEN_ITEM.get());
        int missingTokens = Math.max(0, tokenCost - tokenCount);
        long rawRequiredSpur = (long) missingTokens * SPUR_PER_TOKEN;
        if (rawRequiredSpur > Integer.MAX_VALUE) {
            return false;
        }

        int requiredSpur = (int) rawRequiredSpur;
        int spurCount = getBankSpurBalance(player);
        if (missingTokens > 0 && (!allowSpurConversion || spurCount < requiredSpur)) {
            player.sendSystemMessage(Component.translatable(
                    "incore.cards.vendor.not_enough_currency",
                    tokenCost,
                    tokenCount,
                    missingTokens,
                    requiredSpur,
                    spurCount
            ));
            return false;
        }

        ResourceLocation boosterSetId = null;
        if (offer.productType() == CardVendorOfferData.ProductType.BOOSTER) {
            boosterSetId = CardBoosterManager.resolveSetId(offer.productId());
            if (boosterSetId == null || CardBoosterManager.get(boosterSetId) == null) {
                player.sendSystemMessage(Component.translatable("incore.cards.booster.invalid", offer.productId().toString()));
                return false;
            }
        }

        if (!vendor.consumeStock(offerId, quantity)) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.out_of_stock", quantity, vendor.stockFor(offerId)));
            return false;
        }

        if (requiredSpur > 0 && !deductBankSpur(player, requiredSpur)) {
            vendor.addStock(offerId, quantity);
            int refreshedSpurCount = getBankSpurBalance(player);
            player.sendSystemMessage(Component.translatable(
                    "incore.cards.vendor.not_enough_currency",
                    tokenCost,
                    tokenCount,
                    missingTokens,
                    requiredSpur,
                    refreshedSpurCount
            ));
            return false;
        }

        consumeMatching(player, tokenCost, stack -> stack.getItem() == Registration.CARD_TOKEN_ITEM.get());

        ResourceLocation finalBoosterSetId = boosterSetId;
        ItemStack reward = switch (offer.productType()) {
            case BOOSTER_BOX -> CardItemFactory.boosterBox(offer.productId(), rewardCount);
            case BOOSTER -> CardItemFactory.booster(finalBoosterSetId, rewardCount);
        };

        giveOrDropStacked(player, reward);

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("incore.cards.vendor.purchased", offer.name(), rewardCount));
        return true;
    }

    private static void ensureInventoryInitialized(ServerPlayer player, CardVendorBlockEntity vendor) {
        if (vendor.initialized()) {
            return;
        }

        List<CardVendorOfferData> pool = CardVendorOfferManager.all().stream()
                .sorted(Comparator.comparingInt(CardVendorOfferData::weight).reversed().thenComparing(offer -> offer.id().toString()))
                .toList();

        List<CardVendorOfferData> remaining = new ArrayList<>(pool);
        LinkedHashMap<ResourceLocation, Integer> rolledInventory = new LinkedHashMap<>();
        RandomSource random = player.getRandom();
        int selectionCount = Math.min(OFFERS_PER_VENDOR, remaining.size());

        for (int i = 0; i < selectionCount; i++) {
            int pickedIndex = rollWeightedIndex(remaining, random);
            if (pickedIndex < 0) {
                break;
            }

            CardVendorOfferData selected = remaining.remove(pickedIndex);
            int stock = selected.stockMin();
            if (selected.stockMax() > selected.stockMin()) {
                stock += random.nextInt(selected.stockMax() - selected.stockMin() + 1);
            }
            rolledInventory.put(selected.id(), stock);
        }

        vendor.setInventory(rolledInventory);
    }

    private static int rollWeightedIndex(List<CardVendorOfferData> offers, RandomSource random) {
        if (offers.isEmpty()) {
            return -1;
        }

        int totalWeight = 0;
        for (CardVendorOfferData offer : offers) {
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

    private static CardVendorBlockEntity getVendorBlockEntity(ServerPlayer player, BlockPos pos) {
        if (!player.level().isLoaded(pos)) {
            return null;
        }

        if (player.level().getBlockState(pos).getBlock() != Registration.CARD_VENDOR_BLOCK.get()) {
            return null;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        return blockEntity instanceof CardVendorBlockEntity vendor ? vendor : null;
    }

    private static boolean isWithinVendorRange(ServerPlayer player, BlockPos pos) {
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= MAX_VENDOR_DISTANCE_SQR;
    }

    private static int countMatching(ServerPlayer player, Predicate<ItemStack> predicate) {
        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static int getBankSpurBalance(Player player) {
        return Math.max(0, Numismatics.BANK.getAccount(player).getBalance());
    }

    private static boolean deductBankSpur(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        return Numismatics.BANK.getAccount(player).deduct(amount);
    }

    private static void consumeMatching(ServerPlayer player, int count, Predicate<ItemStack> predicate) {
        int remaining = Math.max(0, count);
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !predicate.test(stack)) {
                continue;
            }

            int consume = Math.min(remaining, stack.getCount());
            stack.shrink(consume);
            remaining -= consume;
            if (stack.isEmpty()) {
                player.getInventory().setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static void giveOrDropStacked(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        int remaining = stack.getCount();
        while (remaining > 0) {
            int nextAmount = Math.min(stack.getMaxStackSize(), remaining);
            ItemStack piece = stack.copyWithCount(nextAmount);
            if (!player.addItem(piece)) {
                player.drop(piece, false);
            }
            remaining -= nextAmount;
        }
    }

    public record VendorScreenData(List<VendorOfferView> offers, int tokenCount, int spurCount, long vendorPosLong) {
    }

    public record VendorOfferView(
            String id,
            String name,
            String productType,
            String productId,
            int count,
            int tokenCost,
            int stockRemaining
    ) {
    }
}
