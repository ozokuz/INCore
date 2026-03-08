package io.github.ozokuz.incore.features.battlepass.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

public record BattlePassSyncPayload(
        boolean hasActiveSet,
        String setId,
        long startsAtMillis,
        long endsAtMillis,
        int currentWeek,
        int totalWeeks,
        int level,
        int xp,
        int xpPerLevel,
        int weeklyCompleted,
        int weeklyCap,
        int permanentCompleted,
        int permanentCap,
        int unclaimedRewardLevels,
        List<LaneEntry> lanes,
        List<TaskEntry> tasks,
        List<RewardLevelEntry> rewardLevels
) implements CustomPacketPayload {
    public static final int REWARD_KIND_ITEM = 0;
    public static final int REWARD_KIND_ENTROPY_CAP = 1;
    public static final int REWARD_KIND_COMMAND = 2;
    public static final int REWARD_KIND_NONE = 3;

    public static final Type<BattlePassSyncPayload> TYPE = new Type<>(ResourceLocation.parse("incore:battle_pass_sync"));
    public static final StreamCodec<ByteBuf, BattlePassSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BattlePassSyncPayload decode(ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            boolean hasActiveSet = buf.readBoolean();
            String setId = buf.readUtf(256);
            long startsAtMillis = buf.readVarLong();
            long endsAtMillis = buf.readVarLong();
            int currentWeek = buf.readVarInt();
            int totalWeeks = buf.readVarInt();
            int level = buf.readVarInt();
            int xp = buf.readVarInt();
            int xpPerLevel = buf.readVarInt();
            int weeklyCompleted = buf.readVarInt();
            int weeklyCap = buf.readVarInt();
            int permanentCompleted = buf.readVarInt();
            int permanentCap = buf.readVarInt();
            int unclaimedRewardLevels = buf.readVarInt();

            int laneCount = buf.readVarInt();
            List<LaneEntry> lanes = new ArrayList<>(laneCount);
            for (int i = 0; i < laneCount; i++) {
                lanes.add(new LaneEntry(
                        buf.readUtf(64),
                        buf.readUtf(64),
                        buf.readBoolean(),
                        buf.readVarInt()
                ));
            }

            int taskCount = buf.readVarInt();
            List<TaskEntry> tasks = new ArrayList<>(taskCount);
            for (int i = 0; i < taskCount; i++) {
                tasks.add(new TaskEntry(
                        buf.readUtf(256),
                        buf.readUtf(1024),
                        buf.readBoolean(),
                        buf.readVarInt(),
                        buf.readUtf(64),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readVarInt(),
                        buf.readBoolean(),
                        buf.readBoolean(),
                        buf.readUtf(256)
                ));
            }

            int rewardLevelCount = buf.readVarInt();
            List<RewardLevelEntry> rewardLevels = new ArrayList<>(rewardLevelCount);
            for (int i = 0; i < rewardLevelCount; i++) {
                int previewLevel = buf.readVarInt();
                int requiredXp = buf.readVarInt();
                int xpForLevel = buf.readVarInt();
                int rewardCount = buf.readVarInt();
                List<RewardEntry> rewards = new ArrayList<>(rewardCount);
                for (int j = 0; j < rewardCount; j++) {
                    rewards.add(new RewardEntry(
                            buf.readVarInt(),
                            buf.readUtf(256),
                            buf.readVarInt(),
                            buf.readUtf(1024)
                    ));
                }
                rewardLevels.add(new RewardLevelEntry(previewLevel, requiredXp, xpForLevel, rewards));
            }

            return new BattlePassSyncPayload(
                    hasActiveSet,
                    setId,
                    startsAtMillis,
                    endsAtMillis,
                    currentWeek,
                    totalWeeks,
                    level,
                    xp,
                    xpPerLevel,
                    weeklyCompleted,
                    weeklyCap,
                    permanentCompleted,
                    permanentCap,
                    unclaimedRewardLevels,
                    lanes,
                    tasks,
                    rewardLevels
            );
        }

        @Override
        public void encode(ByteBuf buffer, BattlePassSyncPayload payload) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeBoolean(payload.hasActiveSet());
            buf.writeUtf(payload.setId(), 256);
            buf.writeVarLong(payload.startsAtMillis());
            buf.writeVarLong(payload.endsAtMillis());
            buf.writeVarInt(payload.currentWeek());
            buf.writeVarInt(payload.totalWeeks());
            buf.writeVarInt(payload.level());
            buf.writeVarInt(payload.xp());
            buf.writeVarInt(payload.xpPerLevel());
            buf.writeVarInt(payload.weeklyCompleted());
            buf.writeVarInt(payload.weeklyCap());
            buf.writeVarInt(payload.permanentCompleted());
            buf.writeVarInt(payload.permanentCap());
            buf.writeVarInt(payload.unclaimedRewardLevels());

            buf.writeVarInt(payload.lanes().size());
            for (LaneEntry lane : payload.lanes()) {
                buf.writeUtf(lane.id(), 64);
                buf.writeUtf(lane.displayName(), 64);
                buf.writeBoolean(lane.unlocked());
                buf.writeVarInt(lane.highestClaimedLevel());
            }

            buf.writeVarInt(payload.tasks().size());
            for (TaskEntry task : payload.tasks()) {
                buf.writeUtf(task.id(), 256);
                buf.writeUtf(task.description(), 1024);
                buf.writeBoolean(task.weekly());
                buf.writeVarInt(task.week());
                buf.writeUtf(task.tier(), 64);
                buf.writeVarInt(task.xpReward());
                buf.writeVarInt(task.progressCurrent());
                buf.writeVarInt(task.progressGoal());
                buf.writeBoolean(task.completed());
                buf.writeBoolean(task.completableNow());
                buf.writeUtf(task.status(), 256);
            }

            buf.writeVarInt(payload.rewardLevels().size());
            for (RewardLevelEntry rewardLevel : payload.rewardLevels()) {
                buf.writeVarInt(rewardLevel.level());
                buf.writeVarInt(rewardLevel.requiredXp());
                buf.writeVarInt(rewardLevel.xpForLevel());
                buf.writeVarInt(rewardLevel.rewards().size());
                for (RewardEntry reward : rewardLevel.rewards()) {
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

    public static void handle(BattlePassSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BattlePassClientCache.update(
                payload.hasActiveSet(),
                payload.setId(),
                payload.startsAtMillis(),
                payload.endsAtMillis(),
                payload.currentWeek(),
                payload.totalWeeks(),
                payload.level(),
                payload.xp(),
                payload.xpPerLevel(),
                payload.weeklyCompleted(),
                payload.weeklyCap(),
                payload.permanentCompleted(),
                payload.permanentCap(),
                payload.unclaimedRewardLevels(),
                payload.lanes().stream()
                        .map(lane -> new BattlePassClientCache.LaneEntry(
                                lane.id(),
                                lane.displayName(),
                                lane.unlocked(),
                                lane.highestClaimedLevel()
                        ))
                        .toList(),
                payload.tasks().stream()
                        .map(task -> new BattlePassClientCache.TaskEntry(
                                task.id(),
                                task.description(),
                                task.weekly(),
                                task.week(),
                                task.tier(),
                                task.xpReward(),
                                task.progressCurrent(),
                                task.progressGoal(),
                                task.completed(),
                                task.completableNow(),
                                task.status()
                        ))
                        .toList(),
                payload.rewardLevels().stream()
                        .map(levelEntry -> new BattlePassClientCache.RewardLevelEntry(
                                levelEntry.level(),
                                levelEntry.requiredXp(),
                                levelEntry.xpForLevel(),
                                levelEntry.rewards().stream()
                                        .map(reward -> new BattlePassClientCache.RewardEntry(
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

    public record LaneEntry(String id, String displayName, boolean unlocked, int highestClaimedLevel) {
    }

    public record TaskEntry(
            String id,
            String description,
            boolean weekly,
            int week,
            String tier,
            int xpReward,
            int progressCurrent,
            int progressGoal,
            boolean completed,
            boolean completableNow,
            String status
    ) {
    }

    public record RewardLevelEntry(int level, int requiredXp, int xpForLevel, List<RewardEntry> rewards) {
    }

    public record RewardEntry(int kind, String iconItemId, int amount, String text) {
    }
}
