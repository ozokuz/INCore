package ozokuz.incore.features.research.station;

public enum ResearchDiskTier {
    T1(2, 0.20D),
    T2(4, 0.12D),
    T3(8, 0.07D),
    T4(16, 0.03D);

    private final int snapshotCapacity;
    private final double corruptionChance;

    ResearchDiskTier(int snapshotCapacity, double corruptionChance) {
        this.snapshotCapacity = snapshotCapacity;
        this.corruptionChance = corruptionChance;
    }

    public int snapshotCapacity() {
        return snapshotCapacity;
    }

    public double corruptionChance() {
        return corruptionChance;
    }
}
