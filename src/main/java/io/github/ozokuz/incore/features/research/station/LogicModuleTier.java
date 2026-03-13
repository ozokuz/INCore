package io.github.ozokuz.incore.features.research.station;

import io.github.ozokuz.incore.features.machines.multiblock.*;

public enum LogicModuleTier {
    T1("basic", 64),
    T2("t2", 96),
    T3("t3", 128),
    T4("t4", 160);

    private final String serializedName;
    private final int durability;

    LogicModuleTier(String serializedName, int durability) {
        this.serializedName = serializedName;
        this.durability = durability;
    }

    public String serializedName() {
        return serializedName;
    }

    public int durability() {
        return durability;
    }

    public static LogicModuleTier fromSerialized(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.strip();
        for (LogicModuleTier tier : values()) {
            if (tier.serializedName.equalsIgnoreCase(normalized)) {
                return tier;
            }
        }
        return null;
    }
}
