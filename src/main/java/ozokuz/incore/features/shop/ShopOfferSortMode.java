package ozokuz.incore.features.shop;

import java.util.Locale;

public enum ShopOfferSortMode {
    ID("id"),
    ROTATION_TIME_REMAINING("rotation_time_remaining");

    private final String serialized;

    ShopOfferSortMode(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopOfferSortMode fromString(String raw) {
        if (raw == null) {
            return ID;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ShopOfferSortMode mode : values()) {
            if (mode.serialized.equals(normalized)) {
                return mode;
            }
        }
        return ID;
    }
}
