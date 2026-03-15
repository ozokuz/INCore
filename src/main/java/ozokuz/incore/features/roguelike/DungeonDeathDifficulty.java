package ozokuz.incore.features.roguelike;

public enum DungeonDeathDifficulty {
    SOFTCORE,
    MEDIUMCORE,
    HARDCORE;

    public static DungeonDeathDifficulty fromString(String raw) {
        if (raw == null) {
            return SOFTCORE;
        }
        try {
            return DungeonDeathDifficulty.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return SOFTCORE;
        }
    }
}
