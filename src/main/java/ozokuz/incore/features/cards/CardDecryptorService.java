package ozokuz.incore.features.cards;

import ozokuz.incore.Registration;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class CardDecryptorService {
    private CardDecryptorService() {
    }

    public static boolean decryptHeld(ServerPlayer player, ItemStack heldStack) {
        if (heldStack.isEmpty() || heldStack.getItem() != Registration.CARD_MODULE_ITEM.get()) {
            player.sendSystemMessage(Component.translatable("incore.cards.decryptor.invalid_held"));
            return false;
        }

        CardItemData.CardInstance instance = CardItemData.readCardInstance(heldStack);
        if (instance == null) {
            player.sendSystemMessage(Component.translatable("incore.cards.decryptor.invalid_held"));
            return false;
        }

        CardModuleData module = CardModuleManager.get(instance.cardId());
        if (module == null || module.moduleType() != CardModuleType.CRYPTIC) {
            player.sendSystemMessage(Component.translatable("incore.cards.decryptor.invalid_held"));
            return false;
        }

        if (instance.revealed()) {
            player.sendSystemMessage(Component.translatable("incore.cards.decryptor.already_revealed", module.name()));
            return false;
        }

        CardItemData.setCardRevealed(heldStack, true);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        player.sendSystemMessage(Component.translatable("incore.cards.decryptor.success", module.name()));
        return true;
    }

    public static int decryptAll(ServerPlayer player) {
        int decrypted = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || stack.getItem() != Registration.CARD_MODULE_ITEM.get()) {
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
            decrypted += 1;
        }

        if (decrypted > 0) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.translatable("incore.cards.decryptor.bulk", decrypted));
            return decrypted;
        }

        player.sendSystemMessage(Component.translatable("incore.cards.decryptor.none"));
        return 0;
    }
}
