package io.github.ozokuz.incore.features.research.station;

public enum OutputPortMode {
    LOGIC,
    DRIVE;

    public OutputPortMode next() {
        return this == LOGIC ? DRIVE : LOGIC;
    }
}
