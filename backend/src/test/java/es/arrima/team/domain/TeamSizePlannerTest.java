package es.arrima.team.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TeamSizePlannerTest {

    @Test
    void evenPlayersFitInDoublettes() {
        TeamPlan plan = TeamSizePlanner.plan(40, 2);

        assertThat(plan.fits()).isTrue();
        assertThat(plan.countOfSize(2)).isEqualTo(20);
    }

    @Test
    void twentyFivePlayersInDoublettesMakeElevenDoublettesAndOneTriplette() {
        TeamPlan plan = TeamSizePlanner.plan(25, 2);

        assertThat(plan.fits()).isFalse();
        assertThat(plan.isPlayable()).isTrue();
        assertThat(plan.countOfSize(2)).isEqualTo(11);
        assertThat(plan.countOfSize(3)).isEqualTo(1);
    }

    @Test
    void twentyFivePlayersInTriplettesMakeSevenTriplettesAndTwoDoublettes() {
        TeamPlan plan = TeamSizePlanner.plan(25, 3);

        assertThat(plan.fits()).isFalse();
        assertThat(plan.countOfSize(3)).isEqualTo(7);
        assertThat(plan.countOfSize(2)).isEqualTo(2);
    }

    @Test
    void twoLeftOverInTriplettesMakeOneDoublette() {
        TeamPlan plan = TeamSizePlanner.plan(26, 3);

        assertThat(plan.countOfSize(3)).isEqualTo(8);
        assertThat(plan.countOfSize(2)).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0} players in teams of {1}: {2} teams, fits={3}")
    @CsvSource({
            "4, 2, 2, true",
            "5, 2, 2, false",  // 1 doublette + 1 triplette
            "6, 3, 2, true",
            "4, 3, 2, false",  // 2 doublettes
            "5, 3, 2, false",  // 1 triplette + 1 doublette
            "7, 3, 3, false",  // 1 triplette + 2 doublettes
            "180, 3, 60, true",
    })
    void smallAndLargeNumbers(int players, int size, int teams, boolean fits) {
        TeamPlan plan = TeamSizePlanner.plan(players, size);

        assertThat(plan.teamCount()).isEqualTo(teams);
        assertThat(plan.fits()).isEqualTo(fits);
        assertThat(plan.teamSizes().stream().mapToInt(Integer::intValue).sum()).isEqualTo(players);
    }

    @ParameterizedTest
    @CsvSource({"0, 2", "1, 2", "3, 2", "2, 3", "3, 3"})
    void tooFewPlayersForTwoTeamsCannotPlay(int players, int size) {
        assertThat(TeamSizePlanner.plan(players, size).isPlayable()).isFalse();
    }
}
