package ozokuz.incore.features.research.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.InvocationTargetException;

public record ResearchStateSyncPayload(String json) implements CustomPacketPayload {
    public static final Type<ResearchStateSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_state_sync"));
    public static final StreamCodec<ByteBuf, ResearchStateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ResearchStateSyncPayload::json,
            ResearchStateSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClient(payload.json()));
    }

    private static void handleClient(String json) {
        try {
            Class<?> handler = Class.forName("ozokuz.incore.features.research.network.ResearchClientPayloadHandlers");
            handler.getMethod("handleSnapshot", String.class).invoke(null, json);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
        }
    }
}
