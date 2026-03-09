package io.github.ozokuz.incore.features.market.network;

import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record RequestOpenMarketScreenPayload(boolean request, String detailItemId) implements CustomPacketPayload {
    public static final Type<RequestOpenMarketScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:request_open_market_screen"));
    public static final StreamCodec<ByteBuf, RequestOpenMarketScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            RequestOpenMarketScreenPayload::request,
            ByteBufCodecs.STRING_UTF8,
            RequestOpenMarketScreenPayload::detailItemId,
            RequestOpenMarketScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenMarketScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!payload.request() || !(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!PlayerFeatureUnlockService.hasUnlocked(player, PlayerFeatureUnlockIds.MARKET_BASIC)) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(PlayerFeatureUnlockIds.MARKET_BASIC));
                return;
            }
            MarketNetworking.openReadOnlyScreenFor(player, parseOptionalItemId(payload.detailItemId()));
        });
    }

    private static @Nullable ResourceLocation parseOptionalItemId(@Nullable String rawItemId) {
        if (rawItemId == null || rawItemId.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(rawItemId);
    }
}
