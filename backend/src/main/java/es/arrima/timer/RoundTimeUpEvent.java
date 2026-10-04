package es.arrima.timer;

/** A round's time ran out (recorded once). The push module tells the subscribed phones. */
public record RoundTimeUpEvent(long meleeId, int roundNumber) {
}
