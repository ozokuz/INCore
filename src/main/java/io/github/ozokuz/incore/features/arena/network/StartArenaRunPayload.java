package io.github.ozokuz.incore.features.arena.network;

import io.github.ozokuz.incore.features.arena.ArenaService;
import io.github.ozokuz.incore.features.arena.data.ArenaCatalogEntry;
import io.github.ozokuz.incore.features.arena.data.ArenaCatalogManager;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StartArenaRunPayload(String entryId) implements CustomPacketPayload {
    public static final Type<StartArenaRunPayload> TYPE = new Type<>(ResourceLocation.parse("incore:start_arena_run"));
    public static final StreamCodec<ByteBuf, StartArenaRunPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            StartArenaRunPayload::entryId,
            StartArenaRunPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartArenaRunPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            ResourceLocation entryId = ResourceLocation.tryParse(payload.entryId());
            if (entryId == null) {
                return;
            }
            ArenaCatalogEntry entry = ArenaCatalogManager.get(entryId);
            if (entry == null) {
                return;
            }
            ResourceLocation requiredUnlock = ArenaService.requiredUnlockForGateway(entry.gatewayId());
            if (!PlayerFeatureUnlockService.hasUnlocked(player, requiredUnlock)) {
                player.sendSystemMessage(PlayerFeatureUnlockService.lockedMessage(requiredUnlock));
                return;
            }

            ArenaNetworking.startRunFor(player, entryId);
        });
    }
}
