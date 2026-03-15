package ozokuz.incore.features.research.model;

public record ResearchPowerDefinition(
        double baseRpPerTick,
        double curveScaleRpPerTick,
        double curveExponent
) {
    public static ResearchPowerDefinition defaults() {
        return new ResearchPowerDefinition(1.0D, 0.0D, 1.0D);
    }
}
