package es.arrima.club;

import jakarta.persistence.Embeddable;

/**
 * Number of courts, rounds (partidas) per team and prizes. The club keeps defaults; each melee
 * stores its own copy, adjusted for that day if needed.
 */
@Embeddable
public record MeleeSettings(int courtCount, int roundsCount, int prizeCount) {

    public static final int MAX_COURTS = 200;
    public static final int MAX_ROUNDS = 20;
    public static final int MAX_PRIZES = 100;

    public static final MeleeSettings DEFAULT = new MeleeSettings(8, 3, 5);
}
