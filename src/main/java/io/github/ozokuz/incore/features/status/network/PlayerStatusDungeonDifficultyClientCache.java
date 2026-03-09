package io.github.ozokuz.incore.features.status.network;

import io.github.ozokuz.incore.features.roguelike.DungeonDeathDifficulty;

public final class PlayerStatusDungeonDifficultyClientCache {
    private static boolean loaded;
    private static DungeonDeathDifficulty difficulty = DungeonDeathDifficulty.SOFTCORE;

    private PlayerStatusDungeonDifficultyClientCache() {
    }

    public static synchronized void update(DungeonDeathDifficulty nextDifficulty) {
        difficulty = nextDifficulty == null ? DungeonDeathDifficulty.SOFTCORE : nextDifficulty;
        loaded = true;
    }

    public static synchronized Snapshot snapshot() {
        return new Snapshot(loaded, difficulty);
    }

    public record Snapshot(boolean loaded, DungeonDeathDifficulty difficulty) {
    }
}
