package io.github.ozokuz.incore.features.battlepass;

import java.util.List;
import java.util.Locale;

public final class BattlePassLane {
    public static final String BASIC = "basic";
    public static final String ORIGINIUM = "originium";
    public static final String PROTOCOL = "protocol";

    private static final List<String> DEFAULT_ORDER = List.of(BASIC, ORIGINIUM, PROTOCOL);

    private BattlePassLane() {
    }

    public static List<String> defaultOrder() {
        return DEFAULT_ORDER;
    }

    public static String normalize(String laneId) {
        if (laneId == null) {
            return "";
        }

        return laneId.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String laneId) {
        return DEFAULT_ORDER.contains(normalize(laneId));
    }

    public static String displayName(String laneId) {
        return switch (normalize(laneId)) {
            case BASIC -> "Basic Supply";
            case ORIGINIUM -> "Originium Supply";
            case PROTOCOL -> "Protocol Customized";
            default -> laneId;
        };
    }
}
