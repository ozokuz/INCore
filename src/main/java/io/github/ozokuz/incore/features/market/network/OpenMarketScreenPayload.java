package io.github.ozokuz.incore.features.market.network;

import io.github.ozokuz.incore.features.market.client.MarketScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenMarketScreenPayload(String json) implements CustomPacketPayload {
    public static final Type<OpenMarketScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:open_market_screen"));
    public static final StreamCodec<ByteBuf, OpenMarketScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenMarketScreenPayload::json,
            OpenMarketScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMarketScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof MarketScreen screen) {
                screen.updatePayload(payload.json());
                return;
            }
            minecraft.setScreen(new MarketScreen(payload.json()));
        });
    }
}
