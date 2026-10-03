package es.arrima.shared.time;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Timestamps are stored in UTC; calendar dates (the day of a melee) are those of Spain.
 */
public final class ArrimaTime {

    public static final ZoneId ZONE = ZoneId.of("Europe/Madrid");

    private ArrimaTime() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(clock.withZone(ZONE));
    }
}
