package io.github.ozokuz.incore.features.vendingmachine;

import io.github.ozokuz.incore.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

public class VendingMachineDiscountCharmItem extends Item implements ICurioItem {
    public VendingMachineDiscountCharmItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return "charm".equals(slotContext.identifier());
    }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext, ItemStack stack) {
        return "charm".equals(slotContext.identifier());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag tooltipFlag
    ) {
        tooltipComponents.add(Component.translatable(
                "incore.vending_machine.discount_charm.tooltip.chance",
                Config.VENDING_MACHINE_DISCOUNT_CURIO_BONUS_CHANCE_PERCENT.get()
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "incore.vending_machine.discount_charm.tooltip.amount",
                Config.VENDING_MACHINE_DISCOUNT_CURIO_BONUS_AMOUNT_PERCENT.get()
        ).withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
