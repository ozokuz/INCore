package io.github.ozokuz.incore.features.assembly.network;

import io.github.ozokuz.incore.features.assembly.content.AssemblyStationBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblyCraftPayload(long blockPos, String recipeId) implements CustomPacketPayload {
    public static final Type<AssemblyCraftPayload> TYPE = new Type<>(ResourceLocation.parse("incore:assembly_craft"));
    public static final StreamCodec<ByteBuf, AssemblyCraftPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            AssemblyCraftPayload::blockPos,
            ByteBufCodecs.STRING_UTF8,
            AssemblyCraftPayload::recipeId,
            AssemblyCraftPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblyCraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = BlockPos.of(payload.blockPos());
            if (player.level().getBlockEntity(pos) instanceof AssemblyStationBlockEntity station && station.canAccess(player)) {
                station.tryCraft(player, ResourceLocation.tryParse(payload.recipeId()));
            }
        });
    }
}
