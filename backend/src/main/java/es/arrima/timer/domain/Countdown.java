package es.arrima.timer.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * The countdown of a round, worked out from what is stored: when it started, how long it lasts and
 * the pauses. Plain arithmetic on instants, so the server and every phone get the same answer for
 * the same moment, and nothing needs to tick on the server.
 *
 * @param pausedBefore total length of the pauses already over
 * @param pausedAt     start of the current pause, or null while it runs
 * @param endedAt      when it ended (time up, or stopped by the next round), or null
 */
public record Countdown(Instant startedAt, Duration duration, Duration pausedBefore, Instant pausedAt, Instant endedAt) {

    public enum State { RUNNING, PAUSED, ENDED }

    public static Countdown start(Instant now, Duration duration) {
        return new Countdown(now, duration, Duration.ZERO, null, null);
    }

    public State state() {
        if (endedAt != null) {
            return State.ENDED;
        }
        return pausedAt != null ? State.PAUSED : State.RUNNING;
    }

    /** When the time runs out if nobody pauses it again: every pause pushes it back. */
    public Instant endsAt() {
        return startedAt.plus(duration).plus(pausedBefore);
    }

    public Duration remaining(Instant now) {
        if (endedAt != null) {
            return Duration.ZERO;
        }
        // While paused, time stands still at the moment of the pause.
        Instant reference = pausedAt != null ? pausedAt : now;
        Duration left = Duration.between(reference, endsAt());
        return left.isNegative() ? Duration.ZERO : left;
    }

    /** Running and out of time: the end still has to be recorded. */
    public boolean isDue(Instant now) {
        return state() == State.RUNNING && !now.isBefore(endsAt());
    }

    public Countdown pause(Instant now) {
        if (state() != State.RUNNING || isDue(now)) {
            throw new IllegalStateException("Only a countdown with time left can be paused");
        }
        return new Countdown(startedAt, duration, pausedBefore, now, null);
    }

    public Countdown resume(Instant now) {
        if (state() != State.PAUSED) {
            throw new IllegalStateException("Only a paused countdown can be resumed");
        }
        return new Countdown(startedAt, duration, pausedBefore.plus(Duration.between(pausedAt, now)), null, null);
    }

    public Countdown end(Instant at) {
        return new Countdown(startedAt, duration, pausedBefore, pausedAt, at);
    }
}
