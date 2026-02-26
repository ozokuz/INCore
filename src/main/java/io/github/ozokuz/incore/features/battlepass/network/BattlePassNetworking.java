package io.github.ozokuz.incore.features.battlepass.network;

import io.github.ozokuz.incore.features.battlepass.BattlePassDefinition;
import io.github.ozokuz.incore.features.battlepass.BattlePassProgressManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassManager;
import io.github.ozokuz.incore.features.battlepass.BattlePassReward;
import io.github.ozokuz.incore.features.sanity.network.SanityNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class BattlePassNetworking {
    private BattlePassNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(BattlePassSyncPayload.TYPE, BattlePassSyncPayload.STREAM_CODEC, BattlePassSyncPayload::handle);
        registrar.playToServer(ClaimBattlePassRewardsPayload.TYPE, ClaimBattlePassRewardsPayload.STREAM_CODEC, ClaimBattlePassRewardsPayload::handle);
    }

    public static void syncToPlayer(ServerPlayer player) {
        Instant now = Instant.now();
        BattlePassProgressManager.ScreenSnapshot snapshot = BattlePassProgressManager.getScreenSnapshot(player, now);

        List<BattlePassSyncPayload.LaneEntry> lanes = snapshot.lanes().stream()
                .map(lane -> new BattlePassSyncPayload.LaneEntry(
                        lane.id(),
                        lane.displayName(),
                        lane.unlocked(),
                        lane.highestClaimedLevel()
                ))
                .toList();

        List<BattlePassSyncPayload.RewardLevelEntry> rewardLevels = List.of();
        Optional<BattlePassDefinition> activeOptional = BattlePassManager.getActiveSet(now);
        if (activeOptional.isPresent()) {
            BattlePassDefinition active = activeOptional.get();
            rewardLevels = active.allConfiguredRewardLevels().stream()
                    .sorted(Comparator.naturalOrder())
                    .map(level -> {
                        int requiredXp = level * active.xpPerLevel();
                        int xpForLevel = level <= 0 ? 0 : active.xpPerLevel();
                        List<BattlePassSyncPayload.RewardEntry> rewards = new ArrayList<>();
                        for (BattlePassSyncPayload.LaneEntry lane : lanes) {
                            List<BattlePassReward> laneRewards = active.rewardsForLevel(lane.id(), level);
                            rewards.add(toPreviewRewardEntry(laneRewards));
                        }
                        return new BattlePassSyncPayload.RewardLevelEntry(level, requiredXp, xpForLevel, rewards);
                    })
                    .toList();
        }

        List<BattlePassSyncPayload.TaskEntry> tasks = new ArrayList<>(snapshot.tasks().size());
        for (BattlePassProgressManager.TaskSnapshot task : snapshot.tasks()) {
            tasks.add(new BattlePassSyncPayload.TaskEntry(
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
            ));
        }

        PacketDistributor.sendToPlayer(player, new BattlePassSyncPayload(
                snapshot.hasActiveSet(),
                snapshot.setId(),
                snapshot.startsAtMillis(),
                snapshot.endsAtMillis(),
                snapshot.currentWeek(),
                snapshot.totalWeeks(),
                snapshot.level(),
                snapshot.xp(),
                snapshot.xpPerLevel(),
                snapshot.weeklyCompleted(),
                snapshot.weeklyCap(),
                snapshot.permanentCompleted(),
                snapshot.permanentCap(),
                snapshot.unclaimedRewardLevels(),
                lanes,
                tasks,
                rewardLevels
        ));
    }

    public static void requestClaimAllRewards() {
        PacketDistributor.sendToServer(new ClaimBattlePassRewardsPayload(true));
    }

    public static BattlePassProgressManager.ClaimResult claimAllRewardsFor(ServerPlayer player) {
        BattlePassProgressManager.ClaimResult result = BattlePassProgressManager.claimAllRewards(player, Instant.now());
        if (result.success()) {
            SanityNetworking.syncToPlayer(player);
        }
        syncToPlayer(player);
        return result;
    }

    private static BattlePassSyncPayload.RewardEntry toPreviewRewardEntry(List<BattlePassReward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return new BattlePassSyncPayload.RewardEntry(
                    BattlePassSyncPayload.REWARD_KIND_NONE,
                    "minecraft:air",
                    0,
                    ""
            );
        }

        return toRewardEntry(rewards.getFirst());
    }

    private static BattlePassSyncPayload.RewardEntry toRewardEntry(BattlePassReward reward) {
        if (reward instanceof BattlePassReward.ItemReward itemReward) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemReward.item());
            return new BattlePassSyncPayload.RewardEntry(
                    BattlePassSyncPayload.REWARD_KIND_ITEM,
                    itemId.toString(),
                    itemReward.count(),
                    itemReward.previewText()
            );
        }

        if (reward instanceof BattlePassReward.SanityCapBonusReward sanityReward) {
            return new BattlePassSyncPayload.RewardEntry(
                    BattlePassSyncPayload.REWARD_KIND_SANITY_CAP,
                    "incore:sanity_vessel",
                    sanityReward.amount(),
                    sanityReward.previewText()
            );
        }

        if (reward instanceof BattlePassReward.CommandReward) {
            return new BattlePassSyncPayload.RewardEntry(
                    BattlePassSyncPayload.REWARD_KIND_COMMAND,
                    "minecraft:command_block",
                    1,
                    reward.previewText()
            );
        }

        return new BattlePassSyncPayload.RewardEntry(
                BattlePassSyncPayload.REWARD_KIND_COMMAND,
                "minecraft:barrier",
                1,
                reward.previewText()
        );
    }
}
