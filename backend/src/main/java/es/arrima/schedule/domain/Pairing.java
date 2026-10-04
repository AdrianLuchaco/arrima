package es.arrima.schedule.domain;

/** Two teams that meet. Stored with the lower id first, so (2, 14) and (14, 2) are the same pairing. */
public record Pairing(long teamA, long teamB) {

    public Pairing {
        if (teamA == teamB) {
            throw new IllegalArgumentException("A team cannot play against itself");
        }
        if (teamA > teamB) {
            long lower = teamB;
            teamB = teamA;
            teamA = lower;
        }
    }

    public boolean involves(long teamId) {
        return teamA == teamId || teamB == teamId;
    }
}
