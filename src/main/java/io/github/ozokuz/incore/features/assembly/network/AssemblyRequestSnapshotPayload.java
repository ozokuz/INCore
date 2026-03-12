package io.github.ozokuz.incore.features.assembly.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyRequestSnapshotPayload(boolean request) implements CustomPacketPayload {
    public static final Type<AssemblyRequestSnapshotPayload> TYPE = new Type<>(ResourceLocation.parse("incore:assembly_request_snapshot"));
    public static final StreamCodec<ByteBuf, AssemblyRequestSnapshotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            AssemblyRequestSnapshotPayload::request,
            AssemblyRequestSnapshotPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblyRequestSnapshotPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.request() && context.player() instanceof ServerPlayer player) {
                AssemblyNetworking.syncToPlayer(player);
            }
        });
    }
}
