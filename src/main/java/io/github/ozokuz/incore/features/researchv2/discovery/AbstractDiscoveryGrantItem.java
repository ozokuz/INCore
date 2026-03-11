package io.github.ozokuz.incore.features.researchv2.discovery;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class AbstractDiscoveryGrantItem extends Item {
    public AbstractDiscoveryGrantItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.success(stack);
        }

        boolean granted = DiscoveryGrantService.grantFromStack(serverPlayer, stack, usedHand);
        return granted ? InteractionResultHolder.consume(stack) : InteractionResultHolder.fail(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        DiscoveryPayload payload = DiscoveryPayloadData.read(stack);
        if (!payload.displayName().isBlank()) {
            tooltipComponents.add(Component.literal(payload.displayName()).withStyle(ChatFormatting.AQUA));
        }
        if (!payload.sourceId().isBlank()) {
            tooltipComponents.add(Component.literal("Source: " + payload.sourceId()).withStyle(ChatFormatting.GRAY));
        }
        if (!payload.originTeamId().isBlank()) {
            tooltipComponents.add(Component.literal("Origin Team: " + payload.originTeamId()).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (!payload.nodeIds().isEmpty()) {
            tooltipComponents.add(Component.literal("Discoveries: " + payload.nodeIds().size()).withStyle(ChatFormatting.GRAY));
        }
    }
}
