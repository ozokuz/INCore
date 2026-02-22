package io.github.ozokuz.incore.features.cards;

import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class DeckItem extends Item implements ICurioItem {
    public DeckItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "deck".equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return "deck".equals(slotContext.identifier());
    }

    @Override
    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(SlotContext slotContext, ResourceLocation id, ItemStack stack) {
        return CardDeckService.resolveDeckModifiers(stack, null);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CardItemData.DeckData deck = CardItemData.readDeckData(stack);
        if (deck == null) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
            return;
        }

        tooltipComponents.add(Component.translatable("incore.cards.deck.tooltip.modules", deck.modules().size()).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("incore.cards.deck.tooltip.integrity", deck.integrity(), deck.maxIntegrity()).withStyle(ChatFormatting.GRAY));
        if (deck.bricked()) {
            tooltipComponents.add(Component.translatable("incore.cards.deck.tooltip.bricked").withStyle(ChatFormatting.DARK_RED));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
