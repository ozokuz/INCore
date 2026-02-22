package io.github.ozokuz.incore.features.cards;

import io.github.ozokuz.incore.Registration;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CardDecryptorService {
    private CardDecryptorService() {
    }

    public static boolean decryptOne(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() != Registration.CARD_MODULE_ITEM.get()) {
                continue;
            }

            CardItemData.CardInstance instance = CardItemData.readCardInstance(stack);
            if (instance == null) {
                continue;
            }

            CardModuleData module = CardModuleManager.get(instance.cardId());
            if (module == null || module.moduleType() != CardModuleType.CRYPTIC || instance.revealed()) {
                continue;
            }

            CardItemData.setCardRevealed(stack, true);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.translatable("incore.cards.decryptor.success", module.name()));
            return true;
        }

        player.sendSystemMessage(Component.translatable("incore.cards.decryptor.none"));
        return false;
    }
}
