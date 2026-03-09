package io.github.ozokuz.incore.features.gacha.network;

import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestOpenGachaBannersPayload(boolean request) implements CustomPacketPayload {
    public static final Type<RequestOpenGachaBannersPayload> TYPE = new Type<>(ResourceLocation.parse("incore:gacha_request_open_banners"));
    public static final StreamCodec<ByteBuf, RequestOpenGachaBannersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestOpenGachaBannersPayload::request,
            RequestOpenGachaBannersPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenGachaBannersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request()) {
                return;
            }

            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!PlayerFeatureUnlockService.hasUnlocked(player, PlayerFeatureUnlockIds.GACHA_BASIC)) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(PlayerFeatureUnlockIds.GACHA_BASIC));
                return;
            }

            GachaNetworking.openBannerScreenFor(player);
        });
    }
}
