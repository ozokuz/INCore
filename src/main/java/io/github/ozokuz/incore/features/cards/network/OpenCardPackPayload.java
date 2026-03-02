package io.github.ozokuz.incore.features.cards.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record OpenCardPackPayload(String json) implements CustomPacketPayload {
    public static final Type<OpenCardPackPayload> TYPE = new Type<>(ResourceLocation.parse("incore:cards_open_pack"));
    public static final StreamCodec<ByteBuf, OpenCardPackPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenCardPackPayload::json,
            OpenCardPackPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenCardPackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClient(payload.json()));
    }

    private static void openClient(String json) {
        try {
            Class<?> handler = Class.forName("io.github.ozokuz.incore.client.features.cards.CardClientPayloadHandlers");
            handler.getMethod("openPackScreen", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
