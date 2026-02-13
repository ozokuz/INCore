package io.github.ozokuz.incore.features.sanity;

import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SanityBoosterItem extends Item {
    private final int restoreAmount;

    public SanityBoosterItem(Properties properties, int restoreAmount) {
        super(properties);
        this.restoreAmount = Math.max(1, restoreAmount);
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

        int cap = SanityManager.getSanityCap(serverPlayer);
        int before = SanityManager.getCurrentSanity(serverPlayer);

        if (before >= cap) {
            serverPlayer.sendSystemMessage(Component.translatable("incore.sanity.booster.full", before, cap));
            return InteractionResultHolder.fail(stack);
        }

        SanityManager.addSanity(serverPlayer, restoreAmount);
        int current = SanityManager.getCurrentSanity(serverPlayer);
        int restored = current - before;

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        SanityNetworking.sendBoosterGainAnimation(serverPlayer, before, current, cap, restored);
        SanityNetworking.syncToPlayer(serverPlayer);
        return InteractionResultHolder.consume(stack);
    }
}
