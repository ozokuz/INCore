package io.github.ozokuz.incore.features.entropy.network;

import io.github.ozokuz.incore.features.entropy.EntropyClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EntropySyncPayload(
        int current,
        int cap,
        int regenPerTick,
        long regenIntervalMillis,
        long millisUntilNextIncrease,
        long millisUntilFull
) implements CustomPacketPayload {
    public static final Type<EntropySyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:entropy_sync"));
    public static final StreamCodec<ByteBuf, EntropySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EntropySyncPayload::current,
            ByteBufCodecs.VAR_INT,
            EntropySyncPayload::cap,
            ByteBufCodecs.VAR_INT,
            EntropySyncPayload::regenPerTick,
            ByteBufCodecs.VAR_LONG,
            EntropySyncPayload::regenIntervalMillis,
            ByteBufCodecs.VAR_LONG,
            EntropySyncPayload::millisUntilNextIncrease,
            ByteBufCodecs.VAR_LONG,
            EntropySyncPayload::millisUntilFull,
            EntropySyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EntropySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EntropyClientCache.update(
                payload.current(),
                payload.cap(),
                payload.regenPerTick(),
                payload.regenIntervalMillis(),
                payload.millisUntilNextIncrease(),
                payload.millisUntilFull()
        ));
    }
}
