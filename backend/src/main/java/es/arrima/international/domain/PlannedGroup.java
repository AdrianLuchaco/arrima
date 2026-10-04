package es.arrima.international.domain;

import java.util.List;

/**
 * Teams with the same number of wins and the prize positions they compete for.
 *
 * @param teamIds       in team-number order, which is also their order of play
 * @param bestPosition  best prize position at stake (1 = first prize)
 * @param worstPosition worst prize position at stake
 */
public record PlannedGroup(int wins, List<Long> teamIds, int bestPosition, int worstPosition) {

    public PlannedGroup {
        teamIds = List.copyOf(teamIds);
    }

    /** A single team does not need to play: its position is already decided. */
    public boolean needsPlay() {
        return teamIds.size() > 1;
    }
}
