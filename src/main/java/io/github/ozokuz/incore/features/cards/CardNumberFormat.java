package io.github.ozokuz.incore.features.cards;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CardNumberFormat {
    private static final double EPSILON = 1.0E-9D;
    private static final int DISPLAY_MAX_DECIMALS = 2;

    private CardNumberFormat() {
    }

    public static String signed(double value) {
        double normalized = Math.abs(value) < EPSILON ? 0.0D : value;
        normalized = round(normalized, DISPLAY_MAX_DECIMALS);
        double rounded = Math.rint(normalized);
        if (Math.abs(normalized - rounded) < EPSILON) {
            return String.format("%+d", (long) rounded);
        }

        BigDecimal stripped = BigDecimal.valueOf(normalized).stripTrailingZeros();
        String sign = stripped.signum() >= 0 ? "+" : "";
        return sign + stripped.toPlainString();
    }

    public static double round(double value, int decimals) {
        return BigDecimal.valueOf(value).setScale(Math.max(0, decimals), RoundingMode.HALF_UP).doubleValue();
    }
}
