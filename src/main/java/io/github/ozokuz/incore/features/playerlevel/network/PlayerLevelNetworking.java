package io.github.ozokuz.incore.features.playerlevel.network;

import io.github.ozokuz.incore.features.playerlevel.PlayerLevelManager;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockDefinition;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockManager;
import io.github.ozokuz.incore.features.playerlevel.PlayerFeatureUnlockService;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelReward;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelRewardManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class PlayerLevelNetworking {
    private static final int LEVEL_PREVIEW_AHEAD = 20;

    private PlayerLevelNetworking() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(PlayerLevelSyncPayload.TYPE, PlayerLevelSyncPayload.STREAM_CODEC, PlayerLevelSyncPayload::handle);
    }

    public static void syncToPlayer(ServerPlayer player) {
        int level = PlayerLevelManager.getLevel(player);
        int currentExperience = PlayerLevelManager.getCurrentExperience(player);
        int experienceToNextLevel = PlayerLevelManager.getExperienceToNextLevel(player);
        int highestRewardLevel = PlayerLevelRewardManager.getHighestRewardLevel();
        int highestUnlockLevel = PlayerFeatureUnlockManager.getHighestRequiredLevel();
        int lastPreviewLevel = Math.max(level + LEVEL_PREVIEW_AHEAD, Math.max(highestRewardLevel, highestUnlockLevel));
        Set<ResourceLocation> unlockedFeatures = PlayerFeatureUnlockService.unlocked(player);

        List<PlayerLevelSyncPayload.RewardPreviewEntry> rewardPreviews = new ArrayList<>();
        for (int previewLevel = level + 1; previewLevel <= lastPreviewLevel; previewLevel++) {
            int requiredExperience = PlayerLevelManager.getExperienceToNextLevel(previewLevel - 1);
            List<PlayerLevelSyncPayload.RewardEntry> rewards = new ArrayList<>();
            PlayerLevelRewardManager.getRewardsForLevel(previewLevel).stream()
                    .map(PlayerLevelNetworking::toRewardEntry)
                    .forEach(rewards::add);
            PlayerFeatureUnlockManager.unlocksForLevel(previewLevel).stream()
                    .map(PlayerLevelNetworking::toFeatureUnlockRewardEntry)
                    .forEach(rewards::add);
            rewardPreviews.add(new PlayerLevelSyncPayload.RewardPreviewEntry(previewLevel, requiredExperience, rewards));
        }

        List<PlayerLevelSyncPayload.FeatureStateEntry> featureStates = PlayerFeatureUnlockManager.all().stream()
                .map(definition -> new PlayerLevelSyncPayload.FeatureStateEntry(
                        definition.id().toString(),
                        definition.requiredLevel(),
                        unlockedFeatures.contains(definition.id()),
                        definition.displayName()
                ))
                .toList();

        PacketDistributor.sendToPlayer(player, new PlayerLevelSyncPayload(
                level,
                currentExperience,
                experienceToNextLevel,
                featureStates,
                rewardPreviews
        ));
    }

    private static PlayerLevelSyncPayload.RewardEntry toRewardEntry(PlayerLevelReward reward) {
        if (reward instanceof PlayerLevelReward.ItemReward itemReward) {
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemReward.item());
            return new PlayerLevelSyncPayload.RewardEntry(
                    PlayerLevelSyncPayload.REWARD_KIND_ITEM,
                    itemId.toString(),
                    itemReward.count(),
                    itemReward.previewText()
            );
        }

        if (reward instanceof PlayerLevelReward.EntropyCapBonusReward entropyReward) {
            return new PlayerLevelSyncPayload.RewardEntry(
                    PlayerLevelSyncPayload.REWARD_KIND_ENTROPY_CAP,
                    "incore:entropy_vessel",
                    entropyReward.amount(),
                    entropyReward.previewText()
            );
        }

        if (reward instanceof PlayerLevelReward.CommandReward commandReward) {
            return new PlayerLevelSyncPayload.RewardEntry(
                    PlayerLevelSyncPayload.REWARD_KIND_COMMAND,
                    "minecraft:command_block",
                    1,
                    commandReward.previewText()
            );
        }

        return new PlayerLevelSyncPayload.RewardEntry(
                PlayerLevelSyncPayload.REWARD_KIND_COMMAND,
                "minecraft:barrier",
                1,
                reward.previewText()
        );
    }

    private static PlayerLevelSyncPayload.RewardEntry toFeatureUnlockRewardEntry(PlayerFeatureUnlockDefinition definition) {
        return new PlayerLevelSyncPayload.RewardEntry(
                PlayerLevelSyncPayload.REWARD_KIND_FEATURE_UNLOCK,
                definition.iconItemId().toString(),
                definition.requiredLevel(),
                definition.displayName()
        );
    }
}
