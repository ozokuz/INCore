package ozokuz.incore.features.shop;

import org.jetbrains.annotations.Nullable;

public enum ShopShowcaseSource {
    TOP_OF_FEED("top_of_feed"),
    ROTATING_FIRST("rotating_first"),
    CATEGORY_PINNED("category_pinned");

    private final String serialized;

    ShopShowcaseSource(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopShowcaseSource fromString(@Nullable String value) {
        if (value != null) {
            for (ShopShowcaseSource source : values()) {
                if (source.serialized.equalsIgnoreCase(value)) {
                    return source;
                }
            }
        }
        return TOP_OF_FEED;
    }
}
