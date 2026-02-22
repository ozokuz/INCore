package io.github.ozokuz.incore.features.cards;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DeckCoreItem extends Item {
    public DeckCoreItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        var id = CardItemData.readDeckCoreId(stack);
        if (id == null) {
            return super.getName(stack);
        }

        CardDeckCoreData core = CardDeckCoreManager.get(id);
        if (core == null) {
            return super.getName(stack);
        }

        return Component.literal(core.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var id = CardItemData.readDeckCoreId(stack);
        if (id != null) {
            CardDeckCoreData core = CardDeckCoreManager.get(id);
            if (core != null) {
                tooltipComponents.add(Component.literal(CardNumberFormat.signed(core.capacityPoints()) + " Capacity").withStyle(ChatFormatting.BLUE));
                tooltipComponents.add(Component.literal(CardNumberFormat.signed(core.baseIntegrity()) + " Integrity").withStyle(ChatFormatting.BLUE));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
