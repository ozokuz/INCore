package io.github.ozokuz.incore.features.researchv2.station;

public enum OutputPortMode {
    LOGIC,
    DRIVE;

    public OutputPortMode next() {
        return this == LOGIC ? DRIVE : LOGIC;
    }
}
