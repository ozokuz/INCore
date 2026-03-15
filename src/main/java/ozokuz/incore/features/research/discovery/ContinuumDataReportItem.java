package ozokuz.incore.features.research.discovery;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class ContinuumDataReportItem extends Item {
    public ContinuumDataReportItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        ResourceLocation reportId = ContinuumDataReportData.readReportId(stack);
        if (reportId != null) {
            tooltipComponents.add(Component.literal("Encoded Report: " + reportId).withStyle(ChatFormatting.GRAY));
        }
    }
}
