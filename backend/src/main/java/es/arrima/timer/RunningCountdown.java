package es.arrima.timer;

import java.time.Instant;

/** A countdown still running: the melee and when its time runs out unless paused. */
public record RunningCountdown(long meleeId, Instant endsAt) {
}
