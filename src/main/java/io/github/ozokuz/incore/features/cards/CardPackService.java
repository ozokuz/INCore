package io.github.ozokuz.incore.features.cards;

import com.google.gson.Gson;
import io.github.ozokuz.incore.features.cards.network.CardNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class CardPackService {
    private static final Gson GSON = new Gson();

    private CardPackService() {
    }

    public static boolean openBoosterFromStack(ServerPlayer player, ItemStack boosterStack) {
        ResourceLocation configuredId = CardItemData.readBoosterId(boosterStack);
        ResourceLocation setId = configuredId == null ? null : CardBoosterManager.resolveSetId(configuredId);
        if (setId == null) {
            setId = CardBoosterManager.getDefaultSetId();
        }

        boolean opened = openBooster(player, setId);
        if (!opened) {
            return false;
        }

        if (!player.isCreative()) {
            boosterStack.shrink(1);
        }
        return true;
    }

    public static boolean openBooster(ServerPlayer player, ResourceLocation setId) {
        CardBoosterData booster = CardBoosterManager.get(setId);
        if (booster == null) {
            player.sendSystemMessage(Component.translatable("incore.cards.booster.invalid", setId.toString()));
            return false;
        }

        CardSetData set = CardSetManager.get(booster.setId());
        if (set == null || !CardSetManager.isSetActive(booster.setId())) {
            player.sendSystemMessage(Component.translatable("incore.cards.set.inactive", booster.setId().toString()));
            return false;
        }

        List<CardModuleData> pool = CardModuleManager.bySet(booster.setId());
        if (pool.isEmpty()) {
            player.sendSystemMessage(Component.translatable("incore.cards.booster.empty", booster.name()));
            return false;
        }

        List<PackRevealEntry> reveals = new ArrayList<>();
        List<CardModuleData> remainingPool = new ArrayList<>(pool);

        for (int i = 0; i < booster.cardsPerPack(); i++) {
            if (remainingPool.isEmpty()) {
                remainingPool = new ArrayList<>(pool);
            }

            CardModuleData module = rollModule(player, remainingPool);
            if (module == null) {
                continue;
            }
            remainingPool.remove(module);

            boolean foil = player.getRandom().nextDouble() < booster.foilChance();
            boolean revealed = module.moduleType() != CardModuleType.CRYPTIC;
            CardChaoticService.ChaoticRoll chaoticRoll = CardChaoticService.roll(module, player.getRandom());

            CardItemData.CardInstance instance = new CardItemData.CardInstance(
                    module.id(),
                    foil,
                    0,
                    false,
                    revealed,
                    1.0D,
                    chaoticRoll.effects(),
                    chaoticRoll.downsides()
            );
            ItemStack cardStack = CardItemFactory.module(instance, 1);

            if (!player.addItem(cardStack)) {
                player.drop(cardStack, false);
            }

            CardCollectionService.addCollected(player, module.id(), foil, 1);
            reveals.add(new PackRevealEntry(module.id().toString(), module.name(), module.rarity(), module.moduleType().name().toLowerCase(), foil));
        }

        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();

        CardNetworking.openPackResults(player, GSON.toJson(new PackRevealScreenData(booster.name(), reveals)));
        player.sendSystemMessage(Component.translatable("incore.cards.booster.opened", booster.name(), reveals.size()));
        return true;
    }

    private static CardModuleData rollModule(ServerPlayer player, List<CardModuleData> pool) {
        int total = 0;
        for (CardModuleData data : pool) {
            total += weightForRarity(data.rarity());
        }

        if (total <= 0) {
            return pool.get(player.getRandom().nextInt(pool.size()));
        }

        int roll = player.getRandom().nextInt(total);
        int cumulative = 0;
        for (CardModuleData data : pool) {
            cumulative += weightForRarity(data.rarity());
            if (roll < cumulative) {
                return data;
            }
        }

        return pool.getLast();
    }

    private static int weightForRarity(int rarity) {
        return switch (Math.clamp(rarity, 1, 6)) {
            case 1 -> 1800;
            case 2 -> 1400;
            case 3 -> 900;
            case 4 -> 350;
            case 5 -> 120;
            case 6 -> 30;
            default -> 100;
        };
    }

    public record PackRevealScreenData(String boosterName, List<PackRevealEntry> pulls) {
    }

    public record PackRevealEntry(String cardId, String cardName, int rarity, String moduleType, boolean foil) {
    }
}
