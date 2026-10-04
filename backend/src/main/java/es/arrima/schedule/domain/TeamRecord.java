package es.arrima.schedule.domain;

/**
 * @param wins    matches won plus the bye, which counts as a win
 * @param pending matches still to be decided: the most wins it can still add
 */
public record TeamRecord(long teamId, int wins, int losses, int pending) {

    public int maxPossibleWins() {
        return wins + pending;
    }
}
