package ozokuz.incore.features.playerlevel.network;

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
        List<FeatureStateEntry> featureStates,
        List<RewardPreviewEntry> rewardPreviews
) implements CustomPacketPayload {
    public static final int REWARD_KIND_ITEM = 0;
    public static final int REWARD_KIND_ENTROPY_CAP = 1;
    public static final int REWARD_KIND_COMMAND = 2;
    public static final int REWARD_KIND_FEATURE_UNLOCK = 3;

    public static final Type<PlayerLevelSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:player_level_sync"));
    public static final StreamCodec<ByteBuf, PlayerLevelSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PlayerLevelSyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            int level = buf.readVarInt();
            int currentExperience = buf.readVarInt();
            int experienceToNextLevel = buf.readVarInt();
            int featureCount = buf.readVarInt();
            List<FeatureStateEntry> featureStates = new ArrayList<>(featureCount);
            for (int i = 0; i < featureCount; i++) {
                featureStates.add(new FeatureStateEntry(
                        buf.readUtf(256),
                        buf.readVarInt(),
                        buf.readBoolean(),
                        buf.readUtf(256)
                ));
            }
            int previewCount = buf.readVarInt();
            List<RewardPreviewEntry> previews = new ArrayList<>(previewCount);

            for (int i = 0; i < previewCount; i++) {
                int previewLevel = buf.readVarInt();
                int requiredExperience = buf.readVarInt();
                int rewardCount = buf.readVarInt();
                List<RewardEntry> rewards = new ArrayList<>(rewardCount);
                for (int j = 0; j < rewardCount; j++) {
                    int kind = buf.readVarInt();
                    String iconItemId = buf.readUtf(256);
                    int amount = buf.readVarInt();
                    String text = buf.readUtf(1024);
                    rewards.add(new RewardEntry(kind, iconItemId, amount, text));
                }
                previews.add(new RewardPreviewEntry(previewLevel, requiredExperience, rewards));
            }

            return new PlayerLevelSyncPayload(level, currentExperience, experienceToNextLevel, featureStates, previews);
        }

        @Override
        public void encode(ByteBuf buffer, PlayerLevelSyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeVarInt(payload.level());
            buf.writeVarInt(payload.currentExperience());
            buf.writeVarInt(payload.experienceToNextLevel());
            buf.writeVarInt(payload.featureStates().size());
            for (FeatureStateEntry featureState : payload.featureStates()) {
                buf.writeUtf(featureState.id(), 256);
                buf.writeVarInt(featureState.requiredLevel());
                buf.writeBoolean(featureState.unlocked());
                buf.writeUtf(featureState.displayName(), 256);
            }
            buf.writeVarInt(payload.rewardPreviews().size());

            for (RewardPreviewEntry preview : payload.rewardPreviews()) {
                buf.writeVarInt(preview.level());
                buf.writeVarInt(preview.requiredExperience());
                buf.writeVarInt(preview.rewards().size());
                for (RewardEntry reward : preview.rewards()) {
                    buf.writeVarInt(reward.kind());
                    buf.writeUtf(reward.iconItemId(), 256);
                    buf.writeVarInt(reward.amount());
                    buf.writeUtf(reward.text(), 1024);
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
                payload.featureStates().stream()
                        .map(feature -> new PlayerLevelClientCache.FeatureState(
                                feature.id(),
                                feature.requiredLevel(),
                                feature.unlocked(),
                                feature.displayName()
                        ))
                        .toList(),
                payload.rewardPreviews().stream()
                        .map(preview -> new PlayerLevelClientCache.RewardPreview(
                                preview.level(),
                                preview.requiredExperience(),
                                preview.rewards().stream()
                                        .map(reward -> new PlayerLevelClientCache.RewardEntry(
                                                reward.kind(),
                                                reward.iconItemId(),
                                                reward.amount(),
                                                reward.text()
                                        ))
                                        .toList()
                        ))
                        .toList()
        ));
    }

    public record FeatureStateEntry(String id, int requiredLevel, boolean unlocked, String displayName) {
    }

    public record RewardPreviewEntry(int level, int requiredExperience, List<RewardEntry> rewards) {
    }

    public record RewardEntry(int kind, String iconItemId, int amount, String text) {
    }
}
