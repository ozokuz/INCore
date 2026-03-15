package ozokuz.incore.features.vendingmachine;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class VendingMachineItemUtil {
    private VendingMachineItemUtil() {
    }

    public static void giveOrDropStacked(ServerPlayer player, ItemStack stack) {
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
}
