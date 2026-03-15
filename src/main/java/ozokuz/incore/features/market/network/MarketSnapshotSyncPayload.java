package ozokuz.incore.features.market.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record MarketSnapshotSyncPayload(String json) implements CustomPacketPayload {
    private static final int MAX_JSON_BYTES = 4 * 1024 * 1024;
    public static final Type<MarketSnapshotSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:market_snapshot_sync"));
    public static final StreamCodec<ByteBuf, MarketSnapshotSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_JSON_BYTES),
            MarketSnapshotSyncPayload::json,
            MarketSnapshotSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MarketSnapshotSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            try {
                Class<?> handler = Class.forName("ozokuz.incore.client.features.market.MarketClientPayloadHandlers");
                handler.getMethod("syncMarketSnapshot", String.class).invoke(null, payload.json());
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
            }
        });
    }
}
