package io.github.ozokuz.incore.features.playerlevel.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record PlayerLevelSyncPayload(
        int level,
        int currentExperience,
        int experienceToNextLevel,
        List<RewardPreviewEntry> rewardPreviews
) implements CustomPacketPayload {
    public static final Type<PlayerLevelSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:player_level_sync"));
    public static final StreamCodec<ByteBuf, PlayerLevelSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerLevelSyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            int level = buf.readVarInt();
            int currentExperience = buf.readVarInt();
            int experienceToNextLevel = buf.readVarInt();
            int previewCount = buf.readVarInt();
            List<RewardPreviewEntry> previews = new ArrayList<>(previewCount);

            for (int i = 0; i < previewCount; i++) {
                int previewLevel = buf.readVarInt();
                int requiredExperience = buf.readVarInt();
                int lineCount = buf.readVarInt();
                List<String> lines = new ArrayList<>(lineCount);
                for (int j = 0; j < lineCount; j++) {
                    lines.add(buf.readUtf(1024));
                }
                previews.add(new RewardPreviewEntry(previewLevel, requiredExperience, lines));
            }

            return new PlayerLevelSyncPayload(level, currentExperience, experienceToNextLevel, previews);
        }

        @Override
        public void encode(ByteBuf buffer, PlayerLevelSyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(payload.level());
            buf.writeVarInt(payload.currentExperience());
            buf.writeVarInt(payload.experienceToNextLevel());
            buf.writeVarInt(payload.rewardPreviews().size());

            for (RewardPreviewEntry preview : payload.rewardPreviews()) {
                buf.writeVarInt(preview.level());
                buf.writeVarInt(preview.requiredExperience());
                buf.writeVarInt(preview.rewards().size());
                for (String line : preview.rewards()) {
                    buf.writeUtf(line, 1024);
                }
            }
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerLevelSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PlayerLevelClientCache.update(
                payload.level(),
                payload.currentExperience(),
                payload.experienceToNextLevel(),
                payload.rewardPreviews().stream()
                        .map(preview -> new PlayerLevelClientCache.RewardPreview(preview.level(), preview.requiredExperience(), preview.rewards()))
                        .toList()
        ));
    }

    public record RewardPreviewEntry(int level, int requiredExperience, List<String> rewards) {
    }
}
