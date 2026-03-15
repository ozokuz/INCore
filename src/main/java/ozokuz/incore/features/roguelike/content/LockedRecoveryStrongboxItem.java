package ozokuz.incore.features.roguelike.content;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class LockedRecoveryStrongboxItem extends BlockItem {
    public LockedRecoveryStrongboxItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable("incore.roguelike.recovery.strongbox.tooltip").withStyle(ChatFormatting.GRAY));
        RecoveryTooltipHelper.appendRecoveryId(stack, tooltipComponents);
    }
}
