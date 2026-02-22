package io.github.ozokuz.incore.features.research;

import io.github.ozokuz.incore.Config;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

public class SpeedModuleCardItem extends Item {
    public SpeedModuleCardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "item.incore.speed_module_card.tooltip.per_card",
                formatPercent(Config.MODULAR_LAB_SPEED_CARD_BONUS.get())
        ).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable(
                "item.incore.speed_module_card.tooltip.max",
                formatPercent(Config.MODULAR_LAB_MAX_SPEED_BONUS.get())
        ).withStyle(ChatFormatting.GRAY));
    }

    private static String formatPercent(double ratio) {
        double percent = Math.max(0.0D, ratio) * 100.0D;
        if (Math.abs(percent - Math.rint(percent)) < 0.0001D) {
            return Integer.toString((int) Math.rint(percent));
        }
        return String.format(Locale.ROOT, "%.1f", percent);
    }
}
