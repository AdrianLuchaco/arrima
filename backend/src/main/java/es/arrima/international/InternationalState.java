package es.arrima.international;

import es.arrima.international.domain.BallSlot;
import es.arrima.international.domain.FinalRanking.RankedTeam;
import es.arrima.international.domain.GroupRanking.GroupStanding;
import es.arrima.international.domain.PlannedGroup;
import java.util.List;

/**
 * La Internacional of a melee as it stands, computed from what is stored.
 *
 * @param plan         every group that holds prize positions (single teams included), from first prize down
 * @param groups       the groups that play, in playing order
 * @param turn         the next ball to throw, or null when there is none
 * @param finalRanking prize positions, only once everything is decided
 */
public record InternationalState(
        List<PlannedGroup> plan,
        List<GroupState> groups,
        CurrentTurn turn,
        List<RankedTeam> finalRanking,
        boolean complete) {

    /** @param teamIds the group's teams in team-number order */
    public record GroupState(InternationalGroup group, List<Long> teamIds, List<RoundState> rounds, GroupStanding standing) {

        /** A tie-break already played that a later correction made meaningless: the admin must know. */
        public boolean hasObsoletePlayedRound() {
            return rounds.stream().anyMatch(round -> round.obsolete() && !round.ballThrows().isEmpty());
        }
    }

    public record RoundState(InternationalRound round, List<BallThrow> ballThrows, boolean obsolete, boolean complete) {

        public int pointsOf(long teamId) {
            return ballThrows.stream().filter(ball -> ball.getTeamId() == teamId).mapToInt(BallThrow::getPoints).sum();
        }
    }

    public record CurrentTurn(long groupId, long roundId, long teamId, BallSlot slot) {
    }
}
