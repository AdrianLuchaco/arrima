package es.arrima.international.domain;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.international.domain.GroupRanking.GroupStanding;
import es.arrima.international.domain.GroupRanking.RoundResult;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GroupRankingTest {

    private static final List<Long> FOUR_TEAMS = List.of(1L, 2L, 3L, 4L);

    @Test
    void beforeAnyBallTheWholeGroupMustPlay() {
        GroupStanding standing = GroupRanking.rank(FOUR_TEAMS, 1, 4, List.of());

        assertThat(standing.tieToBreak()).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(standing.complete()).isFalse();
    }

    @Test
    void aRoundWithoutTiesOrdersTheGroupByPoints() {
        GroupStanding standing = GroupRanking.rank(FOUR_TEAMS, 1, 4,
                List.of(round(10, Map.of(1L, 8, 2L, 15, 3L, 3, 4L, 11))));

        assertThat(standing.order()).containsExactly(2L, 4L, 1L, 3L);
        assertThat(standing.complete()).isTrue();
    }

    @Test
    void aTieForDifferentPrizesNeedsAnotherRoundAmongTheTiedTeamsOnly() {
        // Prizes 1 to 4 at stake: teams 2 and 4 tie for 2nd and 3rd.
        GroupStanding standing = GroupRanking.rank(FOUR_TEAMS, 1, 4,
                List.of(round(10, Map.of(1L, 20, 2L, 12, 3L, 3, 4L, 12))));

        assertThat(standing.tieToBreak()).containsExactlyInAnyOrder(2L, 4L);
        assertThat(standing.complete()).isFalse();
    }

    @Test
    void aTieAcrossTheCutIsBrokenToo() {
        // Only 2 prizes at stake (positions 4 and 5): a tie for 2nd place in the group (5th prize vs nothing).
        GroupStanding standing = GroupRanking.rank(FOUR_TEAMS, 4, 5,
                List.of(round(10, Map.of(1L, 20, 2L, 12, 3L, 12, 4L, 3))));

        assertThat(standing.tieToBreak()).containsExactlyInAnyOrder(2L, 3L);
    }

    @Test
    void aTieThatDecidesNoPrizeIsNotBroken() {
        // Prizes 4 and 5: teams 3 and 4 tie for the group's 3rd and 4th place, outside the prizes.
        GroupStanding standing = GroupRanking.rank(FOUR_TEAMS, 4, 5,
                List.of(round(10, Map.of(1L, 20, 2L, 15, 3L, 6, 4L, 6))));

        assertThat(standing.tieToBreak()).isEmpty();
        assertThat(standing.complete()).isTrue();
        assertThat(standing.order().subList(0, 2)).containsExactly(1L, 2L);
    }

    @Test
    void theTieBreakOrdersOnlyTheTiedTeamsAndIsRepeatedUntilTheyDiffer() {
        RoundResult main = round(10, Map.of(1L, 20, 2L, 12, 3L, 3, 4L, 12));
        RoundResult stillTied = round(11, Map.of(2L, 7, 4L, 7));
        RoundResult decided = round(12, Map.of(2L, 4, 4L, 9));

        GroupStanding afterFirstTieBreak = GroupRanking.rank(FOUR_TEAMS, 1, 4, List.of(main, stillTied));
        assertThat(afterFirstTieBreak.tieToBreak()).containsExactlyInAnyOrder(2L, 4L);

        GroupStanding afterSecond = GroupRanking.rank(FOUR_TEAMS, 1, 4, List.of(main, stillTied, decided));
        // Team 2 had as many main-round points as team 4; the tie-break puts 4 ahead.
        assertThat(afterSecond.order()).containsExactly(1L, 4L, 2L, 3L);
        assertThat(afterSecond.complete()).isTrue();
    }

    @Test
    void whileARoundIsBeingPlayedNoOtherRoundIsAsked() {
        RoundResult main = round(10, Map.of(1L, 20, 2L, 12, 3L, 3, 4L, 12));
        RoundResult tieBreakInProgress = new RoundResult(11, Set.of(2L, 4L), Map.of(2L, 2), false);

        GroupStanding standing = GroupRanking.rank(FOUR_TEAMS, 1, 4, List.of(main, tieBreakInProgress));

        assertThat(standing.tieToBreak()).isEmpty();
        assertThat(standing.complete()).isFalse();
    }

    @Test
    void aCorrectionThatChangesTheTieMakesThePlayedTieBreakObsolete() {
        // Team 4's main round was corrected from 12 to 13: the tie between 2 and 4 no longer exists.
        RoundResult correctedMain = round(10, Map.of(1L, 20, 2L, 12, 3L, 3, 4L, 13));
        RoundResult formerTieBreak = round(11, Map.of(2L, 4, 4L, 9));

        GroupStanding standing = GroupRanking.rank(FOUR_TEAMS, 1, 4, List.of(correctedMain, formerTieBreak));

        assertThat(standing.obsoleteRounds()).containsExactly(11L);
        assertThat(standing.order()).containsExactly(1L, 4L, 2L, 3L);
        assertThat(standing.complete()).isTrue();
    }

    private static RoundResult round(long id, Map<Long, Integer> points) {
        return new RoundResult(id, points.keySet(), points, true);
    }
}
