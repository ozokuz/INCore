package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RecoveryStrongboxKeyItem extends Item {
    public RecoveryStrongboxKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("incore.roguelike.recovery.key.tooltip").withStyle(ChatFormatting.GRAY));
        String recoveryId = stack.get(Registration.RECOVERY_STRONGBOX_ID.get());
        if (recoveryId != null && !recoveryId.isBlank()) {
            tooltipComponents.add(Component.literal(recoveryId).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
