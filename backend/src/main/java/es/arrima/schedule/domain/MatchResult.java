package es.arrima.schedule.domain;

/** @param winner null while the match has not been decided */
public record MatchResult(long teamA, long teamB, Long winner) {

    public boolean isDecided() {
        return winner != null;
    }

    public boolean involves(long teamId) {
        return teamA == teamId || teamB == teamId;
    }
}
