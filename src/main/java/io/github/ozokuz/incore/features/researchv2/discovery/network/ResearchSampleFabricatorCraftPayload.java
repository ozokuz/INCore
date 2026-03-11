package io.github.ozokuz.incore.features.researchv2.discovery.network;

import io.github.ozokuz.incore.features.researchv2.discovery.ResearchSampleFabricatorBlockEntity;
import io.github.ozokuz.incore.features.researchv2.team.ResearchTeamResolver;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchSampleFabricatorCraftPayload(long blockPos, String nodeId) implements CustomPacketPayload {
    public static final Type<ResearchSampleFabricatorCraftPayload> TYPE = new Type<>(ResourceLocation.parse("incore:research_sample_fabricator_craft"));
    public static final StreamCodec<ByteBuf, ResearchSampleFabricatorCraftPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            ResearchSampleFabricatorCraftPayload::blockPos,
            ByteBufCodecs.STRING_UTF8,
            ResearchSampleFabricatorCraftPayload::nodeId,
            ResearchSampleFabricatorCraftPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchSampleFabricatorCraftPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            BlockPos pos = BlockPos.of(payload.blockPos());
            if (!(player.level().getBlockEntity(pos) instanceof ResearchSampleFabricatorBlockEntity fabricator) || !fabricator.canAccess(player)) {
                return;
            }

            ResourceLocation nodeId = ResourceLocation.tryParse(payload.nodeId());
            String teamId = ResearchTeamResolver.resolveTeamId(player);
            if (nodeId == null || teamId == null || teamId.isBlank()) {
                return;
            }
            fabricator.fabricate(teamId, nodeId);
        });
    }
}
