package io.github.ozokuz.incore.features.researchv2.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record ResearchV2StateSyncPayload(String json) implements CustomPacketPayload {
    public static final Type<ResearchV2StateSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_v2_state_sync"));
    public static final StreamCodec<ByteBuf, ResearchV2StateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ResearchV2StateSyncPayload::json,
            ResearchV2StateSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchV2StateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClient(payload.json()));
    }

    private static void handleClient(String json) {
        try {
            Class<?> handler = Class.forName("io.github.ozokuz.incore.features.researchv2.network.ResearchV2ClientPayloadHandlers");
            handler.getMethod("handleSnapshot", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
