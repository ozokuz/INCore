package io.github.ozokuz.incore.features.vendor;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class VendorItemUtil {
    private VendorItemUtil() {
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
