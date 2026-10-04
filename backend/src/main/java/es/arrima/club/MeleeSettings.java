package es.arrima.club;

import jakarta.persistence.Embeddable;

/**
 * Number of courts, rounds (partidas) per team, prizes and entry fee. The club keeps defaults; each
 * melee stores its own copy, adjusted for that day if needed.
 *
 * @param entryFeeCents what each player pays to play, in cents. 0 means the club does not charge
 *                      (or the melee is older than the payment control): nobody is asked to pay.
 */
@Embeddable
public record MeleeSettings(int courtCount, int roundsCount, int prizeCount, int entryFeeCents) {

    public static final int MAX_COURTS = 200;
    public static final int MAX_ROUNDS = 20;
    public static final int MAX_PRIZES = 100;
    public static final int MAX_ENTRY_FEE_CENTS = 10_000;

    public static final MeleeSettings DEFAULT = new MeleeSettings(8, 3, 5, 500);

    /** With a fee, only those who have paid play; without one, everybody signed up does. */
    public boolean requiresPayment() {
        return entryFeeCents > 0;
    }
}
