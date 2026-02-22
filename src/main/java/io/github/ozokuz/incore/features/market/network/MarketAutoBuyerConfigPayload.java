package io.github.ozokuz.incore.features.market.network;

import io.github.ozokuz.incore.features.market.MarketItemManager;
import io.github.ozokuz.incore.features.market.content.MarketAutoBuyerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MarketAutoBuyerConfigPayload(long blockPos, String targetItemId, int priceCapSpur, int batchSize, boolean enabled) implements CustomPacketPayload {
    public static final Type<MarketAutoBuyerConfigPayload> TYPE = new Type<>(ResourceLocation.parse("incore:market_autobuyer_config"));
    public static final StreamCodec<ByteBuf, MarketAutoBuyerConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            MarketAutoBuyerConfigPayload::blockPos,
            ByteBufCodecs.STRING_UTF8,
            MarketAutoBuyerConfigPayload::targetItemId,
            ByteBufCodecs.VAR_INT,
            MarketAutoBuyerConfigPayload::priceCapSpur,
            ByteBufCodecs.VAR_INT,
            MarketAutoBuyerConfigPayload::batchSize,
            ByteBufCodecs.BOOL,
            MarketAutoBuyerConfigPayload::enabled,
            MarketAutoBuyerConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketAutoBuyerConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            BlockPos pos = BlockPos.of(payload.blockPos());
            if (!(player.level().getBlockEntity(pos) instanceof MarketAutoBuyerBlockEntity autoBuyer)) {
                return;
            }
            if (!autoBuyer.canAccess(player)) {
                return;
            }

            String raw = payload.targetItemId() == null ? "" : payload.targetItemId().trim();
            if (raw.isEmpty()) {
                autoBuyer.setTargetItemId(null);
            } else {
                ResourceLocation itemId = ResourceLocation.tryParse(raw);
                if (itemId != null && MarketItemManager.isTradeable(itemId)) {
                    autoBuyer.setTargetItemId(itemId);
                }
            }

            autoBuyer.setPriceCapSpur(payload.priceCapSpur());
            autoBuyer.setBatchSize(payload.batchSize());
            autoBuyer.setEnabled(payload.enabled());
        });
    }
}
