package es.arrima.international.domain;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.international.domain.FinalRanking.RankedTeam;
import es.arrima.international.domain.GroupRanking.GroupStanding;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class FinalRankingTest {

    @Test
    void eachGroupFillsItsPrizePositions() {
        PlannedGroup single = new PlannedGroup(3, List.of(5L), 1, 1);
        PlannedGroup crossing = new PlannedGroup(2, List.of(1L, 2L, 3L), 2, 3);
        GroupStanding crossingStanding = new GroupStanding(List.of(List.of(3L), List.of(1L), List.of(2L)), Set.of(), Set.of(), true);

        List<RankedTeam> ranking = FinalRanking.of(List.of(crossing, single), Map.of(crossing, crossingStanding));

        // Team 2 played but was third in a group fighting for two prizes: no prize.
        assertThat(ranking).containsExactly(new RankedTeam(1, 5), new RankedTeam(2, 3), new RankedTeam(3, 1));
    }
}
