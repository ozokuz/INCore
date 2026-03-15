package ozokuz.incore.features.cards;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class DeckBoxItem extends Item {
    public DeckBoxItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        var id = CardItemData.readDeckBoxId(stack);
        if (id == null) {
            return super.getName(stack);
        }

        CardDeckBoxData box = CardDeckBoxManager.get(id);
        if (box == null) {
            return super.getName(stack);
        }

        return Component.literal(box.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        var id = CardItemData.readDeckBoxId(stack);
        if (id != null) {
            CardDeckBoxData box = CardDeckBoxManager.get(id);
            if (box != null) {
                tooltipComponents.add(Component.literal(CardNumberFormat.signed(box.capacityBonus()) + " Capacity Bonus").withStyle(ChatFormatting.BLUE));
                tooltipComponents.add(Component.literal(CardNumberFormat.signed(box.integrityBonus()) + " Integrity Bonus").withStyle(ChatFormatting.BLUE));
            }
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
