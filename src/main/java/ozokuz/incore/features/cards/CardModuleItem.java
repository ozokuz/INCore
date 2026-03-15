package ozokuz.incore.features.cards;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class CardModuleItem extends Item {
    public CardModuleItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        CardItemData.CardInstance instance = CardItemData.readCardInstance(stack);
        if (instance == null) {
            return super.getName(stack);
        }

        CardModuleData module = CardModuleManager.get(instance.cardId());
        if (module == null) {
            return super.getName(stack);
        }

        return Component.literal(module.name());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CardItemData.CardInstance instance = CardItemData.readCardInstance(stack);
        if (instance == null) {
            super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
            return;
        }

        CardModuleData module = CardModuleManager.get(instance.cardId());
        if (module != null) {
            tooltipComponents.add(Component.literal("Type: " + module.moduleType().name().toLowerCase()).withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("Rarity: " + module.rarity() + "★").withStyle(ChatFormatting.GOLD));
            tooltipComponents.add(Component.literal("Points: " + module.deckPoints()).withStyle(ChatFormatting.YELLOW));
            if (module.moduleType() == CardModuleType.CRYPTIC && !instance.revealed()) {
                tooltipComponents.add(Component.translatable("incore.cards.tooltip.cryptic_hidden").withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                List<CardAttributeEffect> effects = module.moduleType() == CardModuleType.CHAOTIC
                        ? (instance.chaoticEffects().isEmpty() ? module.effects() : instance.chaoticEffects())
                        : module.effects();
                List<CardAttributeEffect> downsides = module.moduleType() == CardModuleType.CHAOTIC
                        ? (instance.chaoticDownsides().isEmpty() ? module.downsides() : instance.chaoticDownsides())
                        : module.downsides();

                appendEffectLines(effects, tooltipComponents, ChatFormatting.GREEN);
                appendEffectLines(downsides, tooltipComponents, ChatFormatting.RED);
            }
        }

        if (instance.foil()) {
            tooltipComponents.add(Component.translatable("incore.cards.tooltip.foil").withStyle(ChatFormatting.AQUA));
        }

        if (instance.graded()) {
            tooltipComponents.add(Component.translatable("incore.cards.tooltip.grade", instance.grade()).withStyle(ChatFormatting.GREEN));
        }

        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        CardItemData.CardInstance instance = CardItemData.readCardInstance(stack);
        return (instance != null && instance.foil()) || super.isFoil(stack);
    }

    private static void appendEffectLines(List<CardAttributeEffect> effects, List<Component> tooltipComponents, ChatFormatting style) {
        for (CardAttributeEffect effect : effects) {
            tooltipComponents.add(formatEffect(effect).withStyle(style));
        }
    }

    private static MutableComponent formatEffect(CardAttributeEffect effect) {
        double amount = effect.amount();
        int opIndex = switch (effect.operation()) {
            case ADD_VALUE -> 0;
            case ADD_MULTIPLIED_BASE -> 1;
            case ADD_MULTIPLIED_TOTAL -> 2;
        };

        if (effect.operation() != AttributeModifier.Operation.ADD_VALUE) {
            amount *= 100.0D;
        }

        String number = CardNumberFormat.signed(Math.abs(amount));
        Component attributeName = Component.literal(CardAttributeResolver.displayName(effect.attributeId()));
        if (amount < 0) {
            return Component.translatable("attribute.modifier.take." + opIndex, number, attributeName);
        }
        return Component.translatable("attribute.modifier.plus." + opIndex, number, attributeName);
    }
}
