package io.github.ozokuz.incore.features.surfaceore.network;

import io.github.ozokuz.incore.features.surfaceore.XaeroWaypointIntegration;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WaypointPayload(
        String name,
        String marker,
        long posPacked
) implements CustomPacketPayload {
    public static final Type<WaypointPayload> TYPE = new Type<>(ResourceLocation.parse("incore:waypoint"));
    public static final StreamCodec<ByteBuf, WaypointPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            WaypointPayload::name,
            ByteBufCodecs.STRING_UTF8,
            WaypointPayload::marker,
            ByteBufCodecs.VAR_LONG,
            WaypointPayload::posPacked,
            WaypointPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaypointPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            BlockPos pos = BlockPos.of(payload.posPacked());
            XaeroWaypointIntegration.addWaypoint(
                    payload.name(),
                    payload.marker(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ()
            );
        });
    }
}
