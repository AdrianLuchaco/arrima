package es.arrima.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The rules every schedule must respect, checked the same way for every generator. */
final class ScheduleInvariants {

    private ScheduleInvariants() {
    }

    static void check(List<Long> teams, List<RoundPairings> schedule, int rounds) {
        assertThat(schedule).hasSize(rounds);
        Set<Pairing> allPairings = new HashSet<>();
        List<Long> byes = new ArrayList<>();

        for (RoundPairings round : schedule) {
            List<Long> appearances = new ArrayList<>();
            for (Pairing pairing : round.pairings()) {
                appearances.add(pairing.teamA());
                appearances.add(pairing.teamB());
                assertThat(allPairings.add(pairing)).as("rematch %s", pairing).isTrue();
            }
            if (teams.size() % 2 == 1) {
                assertThat(round.byeTeam()).as("one team rests each round").isNotNull();
                appearances.add(round.byeTeam());
                byes.add(round.byeTeam());
            } else {
                assertThat(round.byeTeam()).as("nobody rests with an even number of teams").isNull();
            }
            assertThat(appearances).as("every team exactly once in round %d", round.number())
                    .containsExactlyInAnyOrderElementsOf(teams);
        }
        assertThat(byes).as("no team rests twice").doesNotHaveDuplicates();
    }
}
