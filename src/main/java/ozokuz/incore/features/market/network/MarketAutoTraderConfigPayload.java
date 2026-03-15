package ozokuz.incore.features.market.network;

import ozokuz.incore.features.market.MarketItemManager;
import ozokuz.incore.features.market.content.MarketAutoTraderBlockEntity;
import ozokuz.incore.features.market.content.MarketAutoTraderMk2BlockEntity;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockIds;
import ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MarketAutoTraderConfigPayload(long blockPos, String targetItemId, int priceCapSpur, int batchSize) implements CustomPacketPayload {
    public static final Type<MarketAutoTraderConfigPayload> TYPE = new Type<>(ResourceLocation.parse("incore:market_autotrader_config"));
    public static final StreamCodec<ByteBuf, MarketAutoTraderConfigPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            MarketAutoTraderConfigPayload::blockPos,
            ByteBufCodecs.STRING_UTF8,
            MarketAutoTraderConfigPayload::targetItemId,
            ByteBufCodecs.VAR_INT,
            MarketAutoTraderConfigPayload::priceCapSpur,
            ByteBufCodecs.VAR_INT,
            MarketAutoTraderConfigPayload::batchSize,
            MarketAutoTraderConfigPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketAutoTraderConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            BlockPos pos = BlockPos.of(payload.blockPos());
            if (!(player.level().getBlockEntity(pos) instanceof MarketAutoTraderBlockEntity autoTrader)) {
                return;
            }
            if (!autoTrader.canAccess(player)) {
                return;
            }
            ResourceLocation requiredUnlock = autoTrader instanceof MarketAutoTraderMk2BlockEntity
                    ? PlayerFeatureUnlockIds.MARKET_AUTOTRADER_MK2
                    : PlayerFeatureUnlockIds.MARKET_AUTOTRADER;
            if (!PlayerFeatureUnlockService.hasUnlocked(player, requiredUnlock)) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(requiredUnlock));
                return;
            }

            String raw = payload.targetItemId() == null ? "" : payload.targetItemId().trim();
            if (raw.isEmpty()) {
                autoTrader.setTargetItemId(null);
            } else {
                ResourceLocation itemId = ResourceLocation.tryParse(raw);
                if (itemId != null && MarketItemManager.isTradeable(itemId)) {
                    autoTrader.setTargetItemId(itemId);
                }
            }

            autoTrader.setPriceCapSpur(payload.priceCapSpur());
            autoTrader.setBatchSize(payload.batchSize());
        });
    }
}
