package io.github.ozokuz.incore.features.researchv2.station;

public record ResearchStationAugmentSummary(
        double speedMultiplier,
        double powerMultiplier,
        double bonusRunChance,
        double corruptionMultiplier
) {
    public static final ResearchStationAugmentSummary DEFAULT = new ResearchStationAugmentSummary(1.0D, 1.0D, 0.0D, 1.0D);
}
