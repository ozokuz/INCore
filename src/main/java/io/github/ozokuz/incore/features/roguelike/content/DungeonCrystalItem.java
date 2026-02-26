package io.github.ozokuz.incore.features.roguelike.content;

import io.github.ozokuz.incore.Registration;
import io.github.ozokuz.incore.features.roguelike.RoguelikeService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DungeonCrystalItem extends Item {
    public DungeonCrystalItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (context.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }

        if (context.getPlayer() == null || context.getHand() == null) {
            return InteractionResult.PASS;
        }

        boolean activated = RoguelikeService.tryActivatePortalFromFrame(context.getPlayer(), context.getHand(), context.getClickedPos(), context.getClickedFace());
        return activated ? InteractionResult.SUCCESS_NO_ITEM_USED : InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);

        ResourceLocation themeId = stack.get(Registration.DUNGEON_CRYSTAL_THEME.get());
        ResourceLocation objectiveId = stack.get(Registration.DUNGEON_CRYSTAL_OBJECTIVE.get());
        List<ResourceLocation> modifiers = DungeonCrystalDataUtil.readModifiers(stack);

        tooltipComponents.add(
                Component.translatable(
                                "incore.roguelike.crystal.tooltip.theme",
                                themeId == null ? Component.translatable("incore.roguelike.crystal.tooltip.random") : RoguelikeService.themeDisplayName(themeId)
                        )
                        .withStyle(ChatFormatting.AQUA)
        );
        tooltipComponents.add(
                Component.translatable(
                                "incore.roguelike.crystal.tooltip.objective",
                                objectiveId == null ? Component.translatable("incore.roguelike.crystal.tooltip.random") : RoguelikeService.objectiveDisplayName(objectiveId)
                        )
                        .withStyle(ChatFormatting.GOLD)
        );
        if (!modifiers.isEmpty()) {
            List<Component> modifierNames = new ArrayList<>(modifiers.size());
            for (ResourceLocation modifierId : modifiers) {
                modifierNames.add(Component.translatable("incore.roguelike.modifier." + modifierId.getPath()));
            }
            String modifiersText = modifierNames.stream().map(Component::getString).collect(Collectors.joining(", "));
            tooltipComponents.add(
                    Component.translatable("incore.roguelike.crystal.tooltip.modifiers", modifiersText)
                            .withStyle(ChatFormatting.LIGHT_PURPLE)
            );
        }
    }
}
