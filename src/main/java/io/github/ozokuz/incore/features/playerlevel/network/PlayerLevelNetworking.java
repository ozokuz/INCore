package io.github.ozokuz.incore.features.playerlevel.network;

import io.github.ozokuz.incore.features.playerlevel.PlayerLevelManager;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelReward;
import io.github.ozokuz.incore.features.playerlevel.PlayerLevelRewardManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

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
        int lastPreviewLevel = Math.max(level + LEVEL_PREVIEW_AHEAD, highestRewardLevel);

        List<PlayerLevelSyncPayload.RewardPreviewEntry> rewardPreviews = new ArrayList<>();
        for (int previewLevel = level + 1; previewLevel <= lastPreviewLevel; previewLevel++) {
            int requiredExperience = PlayerLevelManager.getExperienceToNextLevel(previewLevel - 1);
            List<String> rewardText = PlayerLevelRewardManager.getRewardsForLevel(previewLevel).stream()
                    .map(PlayerLevelReward::previewText)
                    .toList();
            rewardPreviews.add(new PlayerLevelSyncPayload.RewardPreviewEntry(previewLevel, requiredExperience, rewardText));
        }

        PacketDistributor.sendToPlayer(player, new PlayerLevelSyncPayload(
                level,
                currentExperience,
                experienceToNextLevel,
                rewardPreviews
        ));
    }
}
