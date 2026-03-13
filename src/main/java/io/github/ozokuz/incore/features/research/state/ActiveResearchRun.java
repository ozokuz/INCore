package io.github.ozokuz.incore.features.research.state;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public record ActiveResearchRun(
        String stationId,
        int runTickProgress,
        int runTickRequired,
        int runPowerMultiplierBps,
        int runBonusRunChanceBps,
        int runCorruptionMultiplierBps
) {
    public ActiveResearchRun {
        stationId = stationId == null ? "" : stationId;
        runTickProgress = Math.max(0, runTickProgress);
        runTickRequired = Math.max(1, runTickRequired);
        runPowerMultiplierBps = Math.max(1, runPowerMultiplierBps);
        runBonusRunChanceBps = Math.max(0, runBonusRunChanceBps);
        runCorruptionMultiplierBps = Math.max(1, runCorruptionMultiplierBps);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("stationId", stationId);
        tag.putInt("runTickProgress", runTickProgress);
        tag.putInt("runTickRequired", runTickRequired);
        tag.putInt("runPowerMultiplierBps", runPowerMultiplierBps);
        tag.putInt("runBonusRunChanceBps", runBonusRunChanceBps);
        tag.putInt("runCorruptionMultiplierBps", runCorruptionMultiplierBps);
        return tag;
    }

    public static @Nullable ActiveResearchRun fromTag(CompoundTag tag) {
        String stationId = tag.getString("stationId");
        if (stationId.isBlank()) {
            return null;
        }
        return new ActiveResearchRun(
                stationId,
                Math.max(0, tag.getInt("runTickProgress")),
                Math.max(1, tag.getInt("runTickRequired")),
                Math.max(1, tag.getInt("runPowerMultiplierBps")),
                Math.max(0, tag.getInt("runBonusRunChanceBps")),
                Math.max(1, tag.getInt("runCorruptionMultiplierBps"))
        );
    }

    public ActiveResearchRun withStationId(String nextStationId) {
        return new ActiveResearchRun(
                nextStationId,
                runTickProgress,
                runTickRequired,
                runPowerMultiplierBps,
                runBonusRunChanceBps,
                runCorruptionMultiplierBps
        );
    }

    public ActiveResearchRun withProgress(int nextRunTickProgress) {
        return new ActiveResearchRun(
                stationId,
                nextRunTickProgress,
                runTickRequired,
                runPowerMultiplierBps,
                runBonusRunChanceBps,
                runCorruptionMultiplierBps
        );
    }

    public double powerMultiplier() {
        return runPowerMultiplierBps / 10_000.0D;
    }

    public double bonusRunChance() {
        return Math.max(0.0D, Math.min(0.9D, runBonusRunChanceBps / 10_000.0D));
    }

    public double corruptionMultiplier() {
        return runCorruptionMultiplierBps / 10_000.0D;
    }
}
