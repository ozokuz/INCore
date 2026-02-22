package io.github.ozokuz.incore.features.research;

public enum LabTier {
    BURNER("burner"),
    MECHANICAL("mechanical"),
    MODULAR("modular");

    private final String id;

    LabTier(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}

