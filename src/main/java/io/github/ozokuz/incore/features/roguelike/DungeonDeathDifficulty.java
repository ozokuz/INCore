package io.github.ozokuz.incore.features.roguelike;

public enum DungeonDeathDifficulty {
    SOFTCORE,
    MEDIUMCORE,
    HARDCORE;

    public static DungeonDeathDifficulty fromString(String raw) {
        try {
            return DungeonDeathDifficulty.valueOf(raw);
        } catch (Exception ignored) {
            return SOFTCORE;
        }
    }
}
