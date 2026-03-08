package io.github.ozokuz.incore.features.entropy.network;

import io.github.ozokuz.incore.features.entropy.EntropyClientCache;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EntropyBoosterGainPayload(
        int from,
        int to,
        int cap,
        int gain
) implements CustomPacketPayload {
    public static final Type<EntropyBoosterGainPayload> TYPE = new Type<>(ResourceLocation.parse("incore:entropy_booster_gain"));
    public static final StreamCodec<ByteBuf, EntropyBoosterGainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EntropyBoosterGainPayload::from,
            ByteBufCodecs.VAR_INT,
            EntropyBoosterGainPayload::to,
            ByteBufCodecs.VAR_INT,
            EntropyBoosterGainPayload::cap,
            ByteBufCodecs.VAR_INT,
            EntropyBoosterGainPayload::gain,
            EntropyBoosterGainPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EntropyBoosterGainPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> EntropyClientCache.recordBoosterGainAnimation(
                payload.from(),
                payload.to(),
                payload.cap(),
                payload.gain()
        ));
    }
}
