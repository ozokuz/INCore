package io.github.ozokuz.incore.features.gacha;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class GachaCrateBlockItem extends BlockItem {
    public GachaCrateBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        ResourceLocation bannerId = GachaCrateData.readBannerId(stack);
        if (bannerId != null) {
            tooltipComponents.add(Component.translatable("block.incore.gacha_crate.tooltip.banner", bannerId.toString()));
        }
    }
}
