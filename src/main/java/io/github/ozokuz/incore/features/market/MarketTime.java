package io.github.ozokuz.incore.features.market;

import net.minecraft.server.MinecraftServer;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class MarketTime {
    private static final LocalTime ROLLOVER_TIME = LocalTime.NOON;

    private MarketTime() {
    }

    public static ZoneId serverZone(MinecraftServer server) {
        return ZoneId.systemDefault();
    }

    public static ZonedDateTime now(MinecraftServer server) {
        return ZonedDateTime.now(serverZone(server));
    }

    public static long hourKey(ZonedDateTime time) {
        return time.toEpochSecond() / 3600L;
    }

    public static long noonDayKey(ZonedDateTime time) {
        LocalDate date = time.toLocalDate();
        if (time.toLocalTime().isBefore(ROLLOVER_TIME)) {
            date = date.minusDays(1);
        }
        return date.toEpochDay();
    }

    public static ZonedDateTime timeForHourKey(MinecraftServer server, long hourKey) {
        return Instant.ofEpochSecond(hourKey * 3600L).atZone(serverZone(server));
    }

    public static double hourlyMoveMultiplier(MinecraftServer server, long hourKey) {
        int hour = timeForHourKey(server, hourKey).getHour();
        if (hour == 0) {
            return Math.max(1D, io.github.ozokuz.incore.Config.MARKET_MIDNIGHT_MOVE_MULTIPLIER.get());
        }
        if (hour == 12) {
            return Math.max(1D, io.github.ozokuz.incore.Config.MARKET_NOON_MOVE_MULTIPLIER.get());
        }
        return 1D;
    }
}
