package io.github.ozokuz.incore.features.battlepass;

import io.github.ozokuz.incore.features.battlepass.network.BattlePassNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.time.Instant;

public class BattlePassLaneUnlockItem extends Item {
    private final String laneId;

    public BattlePassLaneUnlockItem(Properties properties, String laneId) {
        super(properties);
        this.laneId = BattlePassLane.normalize(laneId);
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

        BattlePassProgressManager.LaneManagementResult result = BattlePassProgressManager.unlockLane(serverPlayer, laneId, Instant.now());
        serverPlayer.sendSystemMessage(Component.literal(result.message()));
        BattlePassNetworking.syncToPlayer(serverPlayer);

        if (!result.success()) {
            return InteractionResultHolder.fail(stack);
        }

        if (!serverPlayer.isCreative()) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }
}
