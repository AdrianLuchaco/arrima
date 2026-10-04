package es.arrima.melee;

/** The phases of a melee, in order. The admin can go back while it is not closed. */
public enum MeleeStatus {

    REGISTRATION,
    TEAMS,
    MATCHES,
    INTERNATIONAL,
    PRIZES,
    CLOSED;

    public boolean isAtLeast(MeleeStatus other) {
        return ordinal() >= other.ordinal();
    }

    MeleeStatus previous() {
        return values()[ordinal() - 1];
    }
}
