package ozokuz.incore.features.machines.multiblock;

public enum OutputPortMode {
    UNBOUND,
    LOGIC,
    DRIVE;

    public OutputPortMode next() {
        return switch (this) {
            case UNBOUND -> LOGIC;
            case LOGIC -> DRIVE;
            case DRIVE -> UNBOUND;
        };
    }
}
