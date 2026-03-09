package io.github.ozokuz.incore.features.battlepass.network;

import io.github.ozokuz.incore.features.battlepass.BattlePassProgressManager;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ClaimBattlePassRewardsPayload(boolean request) implements CustomPacketPayload {
    public static final Type<ClaimBattlePassRewardsPayload> TYPE = new Type<>(ResourceLocation.parse("incore:battle_pass_claim_rewards"));
    public static final StreamCodec<ByteBuf, ClaimBattlePassRewardsPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ClaimBattlePassRewardsPayload::request,
            ClaimBattlePassRewardsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClaimBattlePassRewardsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request()) {
                return;
            }

            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!PlayerFeatureUnlockService.hasUnlocked(player, PlayerFeatureUnlockIds.BATTLEPASS_SCREEN)) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(PlayerFeatureUnlockIds.BATTLEPASS_SCREEN).withStyle(ChatFormatting.RED));
                return;
            }

            BattlePassProgressManager.ClaimResult result = BattlePassNetworking.claimAllRewardsFor(player);
            if (result.success()) {
                player.sendSystemMessage(Component.literal(
                        result.message() + " Rewards granted: " + result.rewardCount() + "."
                ).withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.literal(result.message()).withStyle(ChatFormatting.RED));
            }
        });
    }
}
