package io.github.ozokuz.incore.features.roguelike.instance;

public record DungeonInstanceId(long value) {
    public DungeonInstanceId {
        if (value <= 0L) {
            throw new IllegalArgumentException("Dungeon instance id must be positive");
        }
    }
}
