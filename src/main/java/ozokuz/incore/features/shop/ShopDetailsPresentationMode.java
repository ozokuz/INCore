package ozokuz.incore.features.shop;

import org.jetbrains.annotations.Nullable;

public enum ShopDetailsPresentationMode {
    INLINE_DOCK("inline"),
    MODAL_OVERLAY("modal");

    private final String serialized;

    ShopDetailsPresentationMode(String serialized) {
        this.serialized = serialized;
    }

    public String serialized() {
        return serialized;
    }

    public static ShopDetailsPresentationMode fromString(@Nullable String value) {
        if (value != null) {
            for (ShopDetailsPresentationMode mode : values()) {
                if (mode.serialized.equalsIgnoreCase(value)) {
                    return mode;
                }
            }
        }
        return INLINE_DOCK;
    }
}
