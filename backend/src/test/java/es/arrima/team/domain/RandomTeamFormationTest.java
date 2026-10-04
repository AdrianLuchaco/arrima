package es.arrima.team.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.LongStream;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class RandomTeamFormationTest {

    private final RandomTeamFormation formation = new RandomTeamFormation();

    @RepeatedTest(20)
    void everyPlayerIsInExactlyOneTeamOfThePlannedSize() {
        List<Long> players = players(25);
        TeamPlan plan = TeamSizePlanner.plan(25, 2);

        List<List<Long>> teams = formation.formTeams(players, plan, new Random());

        assertThat(teams).hasSize(12);
        assertThat(teams.stream().flatMap(List::stream)).containsExactlyInAnyOrderElementsOf(players);
        assertThat(teams.stream().map(List::size)).containsExactlyInAnyOrder(2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3);
    }

    @Test
    void theOddSizedTeamGetsADifferentNumberFromOneDrawToAnother() {
        TeamPlan plan = TeamSizePlanner.plan(25, 3); // 7 triplettes and 2 doublettes
        Set<Integer> positionsOfFirstDoublette = new HashSet<>();

        for (int seed = 0; seed < 50; seed++) {
            List<List<Long>> teams = formation.formTeams(players(25), plan, new Random(seed));
            for (int i = 0; i < teams.size(); i++) {
                if (teams.get(i).size() == 2) {
                    positionsOfFirstDoublette.add(i);
                    break;
                }
            }
        }

        assertThat(positionsOfFirstDoublette).hasSizeGreaterThan(3);
    }

    @Test
    void theSameSeedGivesTheSameDraw() {
        TeamPlan plan = TeamSizePlanner.plan(40, 2);

        assertThat(formation.formTeams(players(40), plan, new Random(7)))
                .isEqualTo(formation.formTeams(players(40), plan, new Random(7)));
    }

    private static List<Long> players(int count) {
        return LongStream.rangeClosed(1, count).boxed().toList();
    }
}
