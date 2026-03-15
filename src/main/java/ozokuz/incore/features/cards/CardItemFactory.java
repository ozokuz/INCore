package ozokuz.incore.features.cards;

import ozokuz.incore.Registration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class CardItemFactory {
    private CardItemFactory() {
    }

    public static ItemStack booster(ResourceLocation boosterId, int count) {
        ItemStack stack = new ItemStack(Registration.CARD_BOOSTER_ITEM.get(), Math.max(1, count));
        CardItemData.writeBoosterId(stack, boosterId);
        return stack;
    }

    public static ItemStack boosterBox(ResourceLocation boxId, int count) {
        ItemStack stack = new ItemStack(Registration.CARD_BOOSTER_BOX_ITEM.get(), Math.max(1, count));
        CardItemData.writeBoosterBoxId(stack, boxId);
        return stack;
    }

    public static ItemStack deckCore(ResourceLocation coreId, int count) {
        ItemStack stack = new ItemStack(Registration.CARD_DECK_CORE_ITEM.get(), Math.max(1, count));
        CardItemData.writeDeckCoreId(stack, coreId);
        return stack;
    }

    public static ItemStack deckBox(ResourceLocation boxId, int count) {
        ItemStack stack = new ItemStack(Registration.CARD_DECK_BOX_ITEM.get(), Math.max(1, count));
        CardItemData.writeDeckBoxId(stack, boxId);
        return stack;
    }

    public static ItemStack module(CardItemData.CardInstance cardInstance, int count) {
        ItemStack stack = new ItemStack(Registration.CARD_MODULE_ITEM.get(), Math.max(1, count));
        CardItemData.writeCardInstance(
                stack,
                cardInstance.cardId(),
                cardInstance.foil(),
                cardInstance.grade(),
                cardInstance.graded(),
                cardInstance.revealed(),
                cardInstance.chaoticMultiplier(),
                cardInstance.chaoticEffects(),
                cardInstance.chaoticDownsides()
        );
        return stack;
    }
}
