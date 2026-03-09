package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

final class RecoveryTooltipHelper {
    private RecoveryTooltipHelper() {
    }

    static void appendRecoveryId(ItemStack stack, List<Component> tooltipComponents) {
        String recoveryId = stack.get(Registration.RECOVERY_STRONGBOX_ID.get());
        if (recoveryId != null && !recoveryId.isBlank()) {
            tooltipComponents.add(Component.literal(recoveryId).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
