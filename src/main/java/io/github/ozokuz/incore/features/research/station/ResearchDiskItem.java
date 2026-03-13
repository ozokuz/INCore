package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ResearchDiskItem extends Item {
    private final ResearchDiskTier tier;

    public ResearchDiskItem(Properties properties, ResearchDiskTier tier) {
        super(properties.stacksTo(1));
        this.tier = tier;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal("Snapshots: " + tier.snapshotCapacity()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Corruption: " + Math.round(tier.corruptionChance() * 100.0D) + "%").withStyle(ChatFormatting.GRAY));
        int corrupted = ResearchDiskData.readSnapshots(stack).stream().mapToInt(snapshot -> snapshot.corruptedSegments().size()).sum();
        if (corrupted > 0) {
            tooltipComponents.add(Component.literal("Corrupted segments: " + corrupted).withStyle(ChatFormatting.RED));
        }
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        ResearchDiskData.initialize(stack, tier);
        return stack;
    }
}
