package es.arrima;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** A clock that tests can move forward (token expiry, grace periods, auto-close...). */
public class MutableClock extends Clock {

    private volatile Instant now = serverLikeNow();

    public void advance(Duration duration) {
        now = now.plus(duration);
    }

    public void set(Instant instant) {
        now = instant;
    }

    public void reset() {
        now = serverLikeNow();
    }

    /**
     * Like the clock of a Linux server: nanoseconds, finer than the microseconds PostgreSQL stores.
     * And always 700 ns past the microsecond, which PostgreSQL rounds up: whatever mixes the two
     * precisions fails every time here, not only now and then on the CI (macOS clocks stop at
     * microseconds).
     */
    private static Instant serverLikeNow() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS).plusNanos(700);
    }

    @Override
    public Instant instant() {
        return now;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return Clock.fixed(now, zone);
    }
}
