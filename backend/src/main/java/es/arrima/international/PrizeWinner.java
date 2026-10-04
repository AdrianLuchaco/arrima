package es.arrima.international;

/**
 * A prize position decided by the matches and la Internacional.
 *
 * @param points points of the team's first Internacional round, null if it did not need to play
 */
public record PrizeWinner(int position, long teamId, Integer points) {
}
