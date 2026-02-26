package io.github.ozokuz.incore.features.research.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record OpenResearchScreenPayload(String json) implements CustomPacketPayload {
    public static final Type<OpenResearchScreenPayload> TYPE = new Type<>(ResourceLocation.parse("incore:open_research_screen"));
    public static final StreamCodec<ByteBuf, OpenResearchScreenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            OpenResearchScreenPayload::json,
            OpenResearchScreenPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenResearchScreenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> openClient(payload.json()));
    }

    private static void openClient(String json) {
        try {
            Class<?> handler = Class.forName("io.github.ozokuz.incore.features.research.client.ResearchClientPayloadHandlers");
            handler.getMethod("openResearchScreen", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
