package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class LockedRecoveryStrongboxItem extends BlockItem {
    public LockedRecoveryStrongboxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("incore.roguelike.recovery.strongbox.tooltip").withStyle(ChatFormatting.GRAY));
        String recoveryId = stack.get(Registration.RECOVERY_STRONGBOX_ID.get());
        if (recoveryId != null && !recoveryId.isBlank()) {
            tooltipComponents.add(Component.literal(recoveryId).withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
