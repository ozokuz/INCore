package io.github.ozokuz.incore.features.researchv2.station;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResearchAugmentItem extends Item {
    private final ResearchAugmentType augmentType;
    private final boolean dungeon;
    private final @Nullable ResourceLocation categoryId;

    public ResearchAugmentItem(Properties properties, ResearchAugmentType augmentType, boolean dungeon, @Nullable ResourceLocation categoryId) {
        super(properties.stacksTo(16));
        this.augmentType = augmentType;
        this.dungeon = dungeon;
        this.categoryId = categoryId;
    }

    public ResearchAugmentType augmentType() {
        return augmentType;
    }

    public boolean isDungeon() {
        return dungeon;
    }

    public @Nullable ResourceLocation categoryId() {
        return categoryId;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.literal(dungeon ? "Dungeon augment" : "Normal augment").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Type: " + augmentType.name()).withStyle(ChatFormatting.GRAY));
        if (categoryId != null) {
            tooltipComponents.add(Component.literal("Category: " + categoryId).withStyle(ChatFormatting.GRAY));
        }
    }
}
