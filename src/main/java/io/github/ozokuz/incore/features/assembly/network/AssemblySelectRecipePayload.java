package io.github.ozokuz.incore.features.assembly.network;

import io.github.ozokuz.incore.features.assembly.content.AutoAssemblerBlockEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record AssemblySelectRecipePayload(long blockPos, String recipeId) implements CustomPacketPayload {
    public static final Type<AssemblySelectRecipePayload> TYPE = new Type<>(ResourceLocation.parse("incore:assembly_select_recipe"));
    public static final StreamCodec<ByteBuf, AssemblySelectRecipePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            AssemblySelectRecipePayload::blockPos,
            ByteBufCodecs.STRING_UTF8,
            AssemblySelectRecipePayload::recipeId,
            AssemblySelectRecipePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AssemblySelectRecipePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockPos pos = BlockPos.of(payload.blockPos());
            if (player.level().getBlockEntity(pos) instanceof AutoAssemblerBlockEntity assembler && assembler.canAccess(player)) {
                assembler.setSelectedRecipeId(ResourceLocation.tryParse(payload.recipeId()), player);
            }
        });
    }
}
