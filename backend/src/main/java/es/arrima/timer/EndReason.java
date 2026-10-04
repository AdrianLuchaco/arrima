package es.arrima.timer;

/** Why a round's countdown ended. */
public enum EndReason {
    /** The time ran out: the alarm sounds and the notifications go out. */
    TIME_UP,
    /** The admin started the next round while this one still had time: it just stops, silently. */
    NEXT_ROUND
}
