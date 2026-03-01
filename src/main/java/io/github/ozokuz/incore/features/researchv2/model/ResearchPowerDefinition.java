package io.github.ozokuz.incore.features.researchv2.model;

public record ResearchPowerDefinition(
        double baseRpPerTick,
        double curveScaleRpPerTick,
        double curveExponent
) {
    public static ResearchPowerDefinition defaults() {
        return new ResearchPowerDefinition(1.0D, 0.0D, 1.0D);
    }
}
