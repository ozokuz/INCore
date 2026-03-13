package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class SignalTransmitterItem extends Item {
    public SignalTransmitterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String channelId = SignalTransmitterData.readChannelId(stack);
        String ownerTeamId = SignalTransmitterData.readOwnerTeamId(stack);
        if (channelId.isBlank() || ownerTeamId.isBlank()) {
            tooltipComponents.add(Component.literal("Unbound transmitter").withStyle(ChatFormatting.GRAY));
            return;
        }
        String shortChannel = channelId.length() > 8 ? channelId.substring(0, 8) : channelId;
        tooltipComponents.add(Component.literal("Team: " + ownerTeamId).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Channel: " + shortChannel).withStyle(ChatFormatting.GRAY));
    }
}
