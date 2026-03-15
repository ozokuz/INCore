package ozokuz.incore.features.research.station;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class OrchestrationDiskItem extends Item {
    public OrchestrationDiskItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String json = OrchestrationDiskData.readJson(stack);
        tooltipComponents.add(Component.literal(json.isBlank() ? "No orchestration state mirrored" : "Contains orchestration state").withStyle(ChatFormatting.GRAY));
    }
}
