package io.github.ozokuz.incore.features.assembly.network;

import io.github.ozokuz.incore.features.assembly.client.AssemblyClientPayloadHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record AssemblyStateSyncPayload(List<String> unlockedRecipeIds) implements CustomPacketPayload {
    public static final Type<AssemblyStateSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:assembly_state_sync"));
    public static final StreamCodec<ByteBuf, AssemblyStateSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8),
            AssemblyStateSyncPayload::unlockedRecipeIds,
            AssemblyStateSyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblyStateSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AssemblyClientPayloadHandlers.handleSnapshot(payload.unlockedRecipeIds()));
    }
}
