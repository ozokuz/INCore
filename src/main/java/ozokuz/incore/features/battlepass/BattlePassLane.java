package ozokuz.incore.features.battlepass;

import java.util.Locale;

public final class BattlePassLane {
    private BattlePassLane() {
    }

    public static String normalize(String laneId) {
        if (laneId == null) {
            return "";
        }

        return laneId.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isValid(String laneId) {
        return BattlePassLaneManager.isValid(normalize(laneId));
    }

    public static String displayName(String laneId) {
        return BattlePassLaneManager.displayName(normalize(laneId));
    }
}
