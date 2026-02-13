package io.github.ozokuz.incore.features.sanity;

import io.github.ozokuz.incore.Config;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SanityVesselItem extends Item {
    public SanityVesselItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        int upgrade = Config.SANITY_CAP_UPGRADE_AMOUNT.get();
        SanityManager.addSanityCapBonus(serverPlayer, upgrade);

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        int cap = SanityManager.getSanityCap(serverPlayer);
        int current = SanityManager.getCurrentSanity(serverPlayer);
        SanityNetworking.syncToPlayer(serverPlayer);
        serverPlayer.sendSystemMessage(Component.translatable("incore.sanity.vessel.used", upgrade, current, cap));
        return InteractionResultHolder.consume(stack);
    }
}
