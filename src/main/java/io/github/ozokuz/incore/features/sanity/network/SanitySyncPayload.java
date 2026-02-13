package io.github.ozokuz.incore.features.sanity.network;

import io.github.ozokuz.incore.features.sanity.SanityClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SanitySyncPayload(
        int current,
        int cap,
        int regenPerTick,
        long regenIntervalMillis,
        long millisUntilNextIncrease,
        long millisUntilFull
) implements CustomPacketPayload {
    public static final Type<SanitySyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:sanity_sync"));
    public static final StreamCodec<ByteBuf, SanitySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SanitySyncPayload::current,
            ByteBufCodecs.VAR_INT,
            SanitySyncPayload::cap,
            ByteBufCodecs.VAR_INT,
            SanitySyncPayload::regenPerTick,
            ByteBufCodecs.VAR_LONG,
            SanitySyncPayload::regenIntervalMillis,
            ByteBufCodecs.VAR_LONG,
            SanitySyncPayload::millisUntilNextIncrease,
            ByteBufCodecs.VAR_LONG,
            SanitySyncPayload::millisUntilFull,
            SanitySyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SanitySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SanityClientCache.update(
                payload.current(),
                payload.cap(),
                payload.regenPerTick(),
                payload.regenIntervalMillis(),
                payload.millisUntilNextIncrease(),
                payload.millisUntilFull()
        ));
    }
}
