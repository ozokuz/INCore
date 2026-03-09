package io.github.ozokuz.incore.features.status.network;

import io.github.ozokuz.incore.features.roguelike.DungeonDeathDifficulty;
import io.github.ozokuz.incore.features.roguelike.state.RoguelikeSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetDungeonDifficultyPayload(String difficulty) implements CustomPacketPayload {
    public static final Type<SetDungeonDifficultyPayload> TYPE =
            new Type<>(ResourceLocation.parse("incore:set_dungeon_difficulty"));
    public static final StreamCodec<ByteBuf, SetDungeonDifficultyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            SetDungeonDifficultyPayload::difficulty,
            SetDungeonDifficultyPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetDungeonDifficultyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.getServer() == null) {
                return;
            }

            DungeonDeathDifficulty difficulty = DungeonDeathDifficulty.fromString(payload.difficulty());
            RoguelikeSavedData.get(player.getServer()).setDungeonDeathDifficulty(player.getUUID(), difficulty);
            PlayerStatusNetworking.syncDungeonDifficultyToPlayer(player);
        });
    }
}
