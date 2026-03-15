package ozokuz.incore.features.vendingmachine;

import dev.ithundxr.createnumismatics.Numismatics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class VendingMachineCurrencyUtil {
    private VendingMachineCurrencyUtil() {
    }

    public static int countItem(ServerPlayer player, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return 0;
        }

        int total = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.is(item)) {
                continue;
            }
            total += stack.getCount();
        }
        return total;
    }

    public static void consumeItem(ServerPlayer player, ResourceLocation itemId, int count) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return;
        }

        int remaining = Math.max(0, count);
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !stack.is(item)) {
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

    public static int getBankSpurBalance(Player player) {
        return Math.max(0, Numismatics.BANK.getAccount(player).getBalance());
    }

    public static boolean deductBankSpur(Player player, int amount) {
        if (amount <= 0) {
            return true;
        }
        return Numismatics.BANK.getAccount(player).deduct(amount);
    }
}
