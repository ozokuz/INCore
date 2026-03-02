package io.github.ozokuz.incore.features.market.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record OpenMarketScreenPayload(String json) implements CustomPacketPayload {
    private static final int MAX_JSON_BYTES = 4 * 1024 * 1024;
    public static final Type<OpenMarketScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:open_market_screen"));
    public static final StreamCodec<ByteBuf, OpenMarketScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_JSON_BYTES),
            OpenMarketScreenPayload::json,
            OpenMarketScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMarketScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClient(payload.json()));
    }

    private static void openClient(String json) {
        try {
            Class<?> handler = Class.forName("io.github.ozokuz.incore.client.features.market.MarketClientPayloadHandlers");
            handler.getMethod("openMarketScreen", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
