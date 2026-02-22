package io.github.ozokuz.incore.features.cards;

import com.google.gson.Gson;
import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.cards.network.CardNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public final class CardVendorService {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation SPUR_ID = ResourceLocation.parse("numismatics:spur");

    private CardVendorService() {
    }

    public static void openVendorScreen(ServerPlayer player) {
        List<CardVendorOfferData> offers = CardVendorOfferManager.all().stream()
                .sorted(Comparator.comparingInt(CardVendorOfferData::weight).reversed().thenComparing(offer -> offer.id().toString()))
                .toList();

        Item spurItem = BuiltInRegistries.ITEM.get(SPUR_ID);
        int spurCount = spurItem == Items.AIR ? 0 : countMatching(player, stack -> stack.getItem() == spurItem);
        int tokenCount = countMatching(player, stack -> stack.getItem() == Registration.CARD_TOKEN_ITEM.get());

        VendorScreenData data = new VendorScreenData(
                offers.stream().map(offer -> new VendorOfferView(
                        offer.id().toString(),
                        offer.name(),
                        offer.productType().name().toLowerCase(),
                        offer.productId().toString(),
                        offer.count(),
                        offer.tokenCost(),
                        offer.spurCost()
                )).toList(),
                tokenCount,
                spurCount
        );

        CardNetworking.openVendorScreen(player, GSON.toJson(data));
    }

    public static boolean purchase(ServerPlayer player, ResourceLocation offerId) {
        CardVendorOfferData offer = CardVendorOfferManager.get(offerId);
        if (offer == null) {
            player.sendSystemMessage(Component.translatable("incore.cards.vendor.invalid_offer", offerId.toString()));
            return false;
        }

        Item spurItem = BuiltInRegistries.ITEM.get(SPUR_ID);
        int tokenCount = countMatching(player, stack -> stack.getItem() == Registration.CARD_TOKEN_ITEM.get());
        int spurCount = spurItem == Items.AIR ? 0 : countMatching(player, stack -> stack.getItem() == spurItem);

        if (tokenCount < offer.tokenCost() || spurCount < offer.spurCost()) {
            player.sendSystemMessage(Component.translatable(
                    "incore.cards.vendor.not_enough_currency",
                    offer.tokenCost(),
                    offer.spurCost(),
                    tokenCount,
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

        consumeMatching(player, offer.tokenCost(), stack -> stack.getItem() == Registration.CARD_TOKEN_ITEM.get());
        if (offer.spurCost() > 0 && spurItem != Items.AIR) {
            consumeMatching(player, offer.spurCost(), stack -> stack.getItem() == spurItem);
        }

        ResourceLocation finalBoosterSetId = boosterSetId;
        ItemStack reward = switch (offer.productType()) {
            case BOOSTER_BOX -> CardItemFactory.boosterBox(offer.productId(), offer.count());
            case BOOSTER -> CardItemFactory.booster(finalBoosterSetId, offer.count());
        };

        if (!player.addItem(reward)) {
            player.drop(reward, false);
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("incore.cards.vendor.purchased", offer.name(), offer.count()));
        return true;
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

    public record VendorScreenData(List<VendorOfferView> offers, int tokenCount, int spurCount) {
    }

    public record VendorOfferView(
            String id,
            String name,
            String productType,
            String productId,
            int count,
            int tokenCost,
            int spurCost
    ) {
    }
}
