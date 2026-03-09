package io.github.ozokuz.incore.features.status.network;

import io.github.ozokuz.incore.features.roguelike.DungeonDeathDifficulty;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DungeonDifficultySyncPayload(String difficulty) implements CustomPacketPayload {
    public static final Type<DungeonDifficultySyncPayload> TYPE =
            new Type<>(ResourceLocation.parse("incore:dungeon_difficulty_sync"));
    public static final StreamCodec<ByteBuf, DungeonDifficultySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            DungeonDifficultySyncPayload::difficulty,
            DungeonDifficultySyncPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DungeonDifficultySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerStatusDungeonDifficultyClientCache.update(
                DungeonDeathDifficulty.fromString(payload.difficulty())
        ));
    }
}
