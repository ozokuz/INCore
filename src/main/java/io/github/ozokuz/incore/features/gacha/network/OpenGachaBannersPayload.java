package io.github.ozokuz.incore.features.gacha.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record OpenGachaBannersPayload(String json) implements CustomPacketPayload {
    public static final Type<OpenGachaBannersPayload> TYPE = new Type<>(ResourceLocation.parse("incore:gacha_open_banners"));
    public static final StreamCodec<ByteBuf, OpenGachaBannersPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenGachaBannersPayload::json,
            OpenGachaBannersPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenGachaBannersPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClient(payload.json()));
    }

    private static void openClient(String json) {
        try {
            Class<?> handler = Class.forName("io.github.ozokuz.incore.features.gacha.client.GachaClientPayloadHandlers");
            handler.getMethod("openBannersScreen", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
