package ozokuz.incore.features.battlepass;

import net.minecraft.server.MinecraftServer;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.zone.ZoneOffsetTransition;
import java.util.List;

public final class BattlePassWeekTime {
    private static final LocalTime WEEK_START_TIME = LocalTime.NOON;

    private BattlePassWeekTime() {
    }

    public static ZonedDateTime now(MinecraftServer server) {
        return ZonedDateTime.now(serverZone(server));
    }

    public static ZoneId serverZone(MinecraftServer server) {
        return ZoneId.systemDefault();
    }

    public static ZonedDateTime weekStart(ZonedDateTime time) {
        ZoneId zone = time.getZone();
        LocalDate date = time.toLocalDate();
        int dayDelta = Math.floorMod(date.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue(), 7);
        LocalDate monday = date.minusDays(dayDelta);

        ZonedDateTime mondayNoon = resolveLocalDateTime(zone, monday.atTime(WEEK_START_TIME));
        if (time.isBefore(mondayNoon)) {
            mondayNoon = resolveLocalDateTime(zone, monday.minusWeeks(1).atTime(WEEK_START_TIME));
        }

        return mondayNoon.withNano(0);
    }

    public static ZonedDateTime nextWeekStart(ZonedDateTime time) {
        return weekStart(time).plusWeeks(1);
    }

    public static long weekKey(ZonedDateTime time) {
        return weekStart(time).toInstant().toEpochMilli();
    }

    private static ZonedDateTime resolveLocalDateTime(ZoneId zone, LocalDateTime localDateTime) {
        List<java.time.ZoneOffset> offsets = zone.getRules().getValidOffsets(localDateTime);
        if (!offsets.isEmpty()) {
            return ZonedDateTime.ofLocal(localDateTime, zone, offsets.get(offsets.size() - 1)).withLaterOffsetAtOverlap();
        }

        ZoneOffsetTransition transition = zone.getRules().getTransition(localDateTime);
        if (transition != null) {
            return transition.getDateTimeAfter().atZone(zone);
        }

        return localDateTime.atZone(zone);
    }
}
