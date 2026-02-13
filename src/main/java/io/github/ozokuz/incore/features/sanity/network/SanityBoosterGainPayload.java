package io.github.ozokuz.incore.features.sanity.network;

import io.github.ozokuz.incore.features.sanity.SanityClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SanityBoosterGainPayload(
        int from,
        int to,
        int cap,
        int gain
) implements CustomPacketPayload {
    public static final Type<SanityBoosterGainPayload> TYPE = new Type<>(ResourceLocation.parse("incore:sanity_booster_gain"));
    public static final StreamCodec<ByteBuf, SanityBoosterGainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SanityBoosterGainPayload::from,
            ByteBufCodecs.VAR_INT,
            SanityBoosterGainPayload::to,
            ByteBufCodecs.VAR_INT,
            SanityBoosterGainPayload::cap,
            ByteBufCodecs.VAR_INT,
            SanityBoosterGainPayload::gain,
            SanityBoosterGainPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SanityBoosterGainPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SanityClientCache.recordBoosterGainAnimation(
                payload.from(),
                payload.to(),
                payload.cap(),
                payload.gain()
        ));
    }
}
