package ozokuz.incore.features.tasks.network;

import ozokuz.incore.client.features.tasks.TaskClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record TaskSyncPayload(String json) implements CustomPacketPayload {
    public static final Type<TaskSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:task_sync"));
    public static final StreamCodec<ByteBuf, TaskSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TaskSyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            return new TaskSyncPayload(buf.readUtf(32767));
        }

        @Override
        public void encode(ByteBuf buffer, TaskSyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeUtf(payload.json(), 32767);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TaskSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TaskClientCache.update(payload.json()));
    }
}
