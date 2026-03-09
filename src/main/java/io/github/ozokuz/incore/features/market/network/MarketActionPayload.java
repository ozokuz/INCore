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

public record MarketActionPayload(int action, long terminalPos, String itemId, int quantity) implements CustomPacketPayload {
    public static final int ACTION_REFRESH = 0;
    public static final int ACTION_BUY = 1;
    public static final int ACTION_SELL = 2;

    public static final Type<MarketActionPayload> TYPE = new Type<>(ResourceLocation.parse("incore:market_action"));
    public static final StreamCodec<ByteBuf, MarketActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            MarketActionPayload::action,
            ByteBufCodecs.VAR_LONG,
            MarketActionPayload::terminalPos,
            ByteBufCodecs.STRING_UTF8,
            MarketActionPayload::itemId,
            ByteBufCodecs.VAR_INT,
            MarketActionPayload::quantity,
            MarketActionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!PlayerFeatureUnlockService.hasUnlocked(player, PlayerFeatureUnlockIds.MARKET_BASIC)) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(PlayerFeatureUnlockIds.MARKET_BASIC));
                return;
            }
            MarketNetworking.handleAction(player, payload);
        });
    }
}
