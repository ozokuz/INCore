package io.github.ozokuz.incore.features.shop;

import java.util.Locale;

public enum ShopStockMode {
    NONE("none"),
    PER_ITEM("per_item"),
    CATEGORY_BUCKET("category_bucket");

    private final String serialized;

    ShopStockMode(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopStockMode fromString(String raw) {
        if (raw == null) {
            return NONE;
        }

        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (ShopStockMode mode : values()) {
            if (mode.serialized.equals(normalized)) {
                return mode;
            }
        }
        return NONE;
    }
}
