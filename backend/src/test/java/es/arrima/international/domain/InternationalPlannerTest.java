package es.arrima.international.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The four examples of the specification, with 5 prizes, plus edge cases. */
class InternationalPlannerTest {

    @Test
    void threeTeamsWithThreeWinsAndFiveWithTwo() {
        List<TeamWins> teams = teams(3, 3, 5, 2, 4, 1);

        List<PlannedGroup> groups = InternationalPlanner.plan(teams, 5);

        assertThat(groups).containsExactly(
                new PlannedGroup(3, List.of(1L, 2L, 3L), 1, 3),
                new PlannedGroup(2, List.of(4L, 5L, 6L, 7L, 8L), 4, 5));
        // The group with fewer wins plays first.
        assertThat(InternationalPlanner.playingOrder(groups)).extracting(PlannedGroup::wins).containsExactly(2, 3);
    }

    @Test
    void fiveTeamsWithThreeWinsPlayOnlyToBeOrdered() {
        List<PlannedGroup> groups = InternationalPlanner.plan(teams(5, 3, 6, 2), 5);

        assertThat(groups).containsExactly(new PlannedGroup(3, List.of(1L, 2L, 3L, 4L, 5L), 1, 5));
    }

    @Test
    void sevenTeamsWithThreeWinsPlayForTheFivePrizesAndTheRestDoNotPlay() {
        List<PlannedGroup> groups = InternationalPlanner.plan(teams(7, 3, 5, 2), 5);

        assertThat(groups).containsExactly(new PlannedGroup(3, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L), 1, 5));
    }

    @Test
    void aSingleTeamWithThreeWinsIsFirstWithoutPlaying() {
        List<PlannedGroup> groups = InternationalPlanner.plan(teams(1, 3, 6, 2), 5);

        assertThat(groups.getFirst()).isEqualTo(new PlannedGroup(3, List.of(1L), 1, 1));
        assertThat(groups.getFirst().needsPlay()).isFalse();
        assertThat(groups.get(1)).isEqualTo(new PlannedGroup(2, List.of(2L, 3L, 4L, 5L, 6L, 7L), 2, 5));
        assertThat(InternationalPlanner.playingOrder(groups)).extracting(PlannedGroup::wins).containsExactly(2);
    }

    @Test
    void withFewerTeamsThanPrizesEveryTeamGetsOne() {
        List<PlannedGroup> groups = InternationalPlanner.plan(teams(2, 1, 1, 0), 5);

        assertThat(groups).containsExactly(
                new PlannedGroup(1, List.of(1L, 2L), 1, 2),
                new PlannedGroup(0, List.of(3L), 3, 3));
    }

    @Test
    void teamsInAGroupAreOrderedByTeamNumber() {
        List<TeamWins> teams = List.of(new TeamWins(30, 3, 2), new TeamWins(10, 1, 2), new TeamWins(20, 2, 2));

        assertThat(InternationalPlanner.plan(teams, 5).getFirst().teamIds()).containsExactly(10L, 20L, 30L);
    }

    /** Pairs (count, wins): teams numbered from 1 in order, their id equal to their number. */
    private static List<TeamWins> teams(int... countAndWins) {
        List<TeamWins> teams = new ArrayList<>();
        int number = 1;
        for (int i = 0; i < countAndWins.length; i += 2) {
            for (int j = 0; j < countAndWins[i]; j++, number++) {
                teams.add(new TeamWins(number, number, countAndWins[i + 1]));
            }
        }
        return teams;
    }
}
