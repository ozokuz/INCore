package io.github.ozokuz.incore.features.shop;

import java.util.Locale;

public enum ShopReplenishMode {
    NONE("none"),
    DAILY_NOON("daily_noon"),
    GACHA_ROTATION("gacha_rotation");

    private final String serialized;

    ShopReplenishMode(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopReplenishMode fromString(String raw) {
        if (raw == null) {
            return NONE;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ShopReplenishMode mode : values()) {
            if (mode.serialized.equals(normalized)) {
                return mode;
            }
        }
        return NONE;
    }
}
