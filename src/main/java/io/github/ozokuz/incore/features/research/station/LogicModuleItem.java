package io.github.ozokuz.incore.features.research.station;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class LogicModuleItem extends Item {
    private final LogicModuleTier tier;
    private final boolean fresh;
    private final String stateLabel;

    public LogicModuleItem(Properties properties, LogicModuleTier tier, boolean fresh, String stateLabel) {
        super(fresh ? properties.stacksTo(1).durability(tier.durability()) : properties.stacksTo(64));
        this.tier = tier;
        this.fresh = fresh;
        this.stateLabel = stateLabel;
    }

    public LogicModuleTier tier() {
        return tier;
    }

    public boolean isFresh() {
        return fresh;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Tier: " + tier.serializedName().toUpperCase()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("State: " + stateLabel).withStyle(ChatFormatting.GRAY));
    }
}
