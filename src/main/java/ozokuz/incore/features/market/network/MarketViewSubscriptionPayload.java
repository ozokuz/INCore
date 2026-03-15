package ozokuz.incore.features.market.network;

import ozokuz.incore.features.market.MarketService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record MarketViewSubscriptionPayload(boolean subscribed, boolean hasTerminalPos, long terminalPos, String detailItemId)
        implements CustomPacketPayload {
    public static final Type<MarketViewSubscriptionPayload> TYPE = new Type<>(ResourceLocation.parse("incore:market_view_subscription"));
    public static final StreamCodec<ByteBuf, MarketViewSubscriptionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            MarketViewSubscriptionPayload::subscribed,
            ByteBufCodecs.BOOL,
            MarketViewSubscriptionPayload::hasTerminalPos,
            ByteBufCodecs.VAR_LONG,
            MarketViewSubscriptionPayload::terminalPos,
            ByteBufCodecs.STRING_UTF8,
            MarketViewSubscriptionPayload::detailItemId,
            MarketViewSubscriptionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketViewSubscriptionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            if (!payload.subscribed()) {
                MarketService.unsubscribeViewer(player);
                return;
            }

            MarketService.subscribeViewer(
                    player,
                    payload.hasTerminalPos() ? payload.terminalPos() : null,
                    parseOptionalItemId(payload.detailItemId())
            );
        });
    }

    private static @Nullable ResourceLocation parseOptionalItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw.trim());
    }
}
