package io.github.ozokuz.incore.features.arena.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record OpenArenaCatalogPayload(String json) implements CustomPacketPayload {
    public static final Type<OpenArenaCatalogPayload> TYPE = new Type<>(ResourceLocation.parse("incore:open_arena_catalog"));
    public static final StreamCodec<ByteBuf, OpenArenaCatalogPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenArenaCatalogPayload::json,
            OpenArenaCatalogPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenArenaCatalogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClient(payload.json()));
    }

    private static void openClient(String json) {
        try {
            Class<?> handler = Class.forName("io.github.ozokuz.incore.client.arena.ArenaClientPayloadHandlers");
            handler.getMethod("openCatalogScreen", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
