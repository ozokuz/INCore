package io.github.ozokuz.incore.features.tasks.network;

import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TaskClaimRewardsPayload(int claimType) implements CustomPacketPayload {
    public static final int CLAIM_DAILY = 0;
    public static final int CLAIM_WEEKLY_UNLOCKED = 1;

    public static final Type<TaskClaimRewardsPayload> TYPE = new Type<>(ResourceLocation.parse("incore:task_claim_rewards"));
    public static final StreamCodec<ByteBuf, TaskClaimRewardsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            TaskClaimRewardsPayload::claimType,
            TaskClaimRewardsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TaskClaimRewardsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!PlayerFeatureUnlockService.hasUnlocked(player, PlayerFeatureUnlockIds.TASKS_SCREEN)) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(PlayerFeatureUnlockIds.TASKS_SCREEN));
                return;
            }

            if (payload.claimType() == CLAIM_DAILY) {
                TaskNetworking.applyDailyRewardClaim(player);
                return;
            }

            if (payload.claimType() == CLAIM_WEEKLY_UNLOCKED) {
                TaskNetworking.applyWeeklyRewardsClaim(player);
            }
        });
    }
}
