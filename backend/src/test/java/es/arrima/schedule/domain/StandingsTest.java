package es.arrima.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class StandingsTest {

    @Test
    void theByeCountsAsAWin() {
        List<TeamRecord> records = Standings.records(List.of(1L, 2L, 3L),
                List.of(new MatchResult(1, 2, 1L)), List.of(3L));

        assertThat(records).containsExactly(
                new TeamRecord(1, 1, 0, 0),
                new TeamRecord(2, 0, 1, 0),
                new TeamRecord(3, 1, 0, 0));
    }

    @Test
    void undecidedMatchesArePending() {
        List<TeamRecord> records = Standings.records(List.of(1L, 2L),
                List.of(new MatchResult(1, 2, null)), List.of());

        assertThat(records).containsExactly(new TeamRecord(1, 0, 0, 1), new TeamRecord(2, 0, 0, 1));
    }

    @Test
    void theCounterAtTheStartSaysEveryoneCanStillReachEveryFigure() {
        List<TeamRecord> records = List.of(new TeamRecord(1, 0, 0, 4), new TeamRecord(2, 0, 0, 4));

        assertThat(Standings.counter(records, 4)).containsExactly(new WinTarget(4, 0, 2), new WinTarget(3, 0, 2));
    }

    @Test
    void theCounterMidwayReadsLikeThePaperSheet() {
        // 4 rounds. After three of them:
        List<TeamRecord> records = List.of(
                new TeamRecord(1, 3, 0, 1),  // 3 wins, can still get 4
                new TeamRecord(2, 3, 0, 1),  // idem
                new TeamRecord(3, 2, 1, 1),  // can reach 3, not 4
                new TeamRecord(4, 1, 2, 1),  // can reach neither
                new TeamRecord(5, 4, 0, 0),  // finished with 4 (the bye included)
                new TeamRecord(6, 3, 1, 0)); // finished with 3

        assertThat(Standings.counter(records, 4)).containsExactly(
                new WinTarget(4, 1, 2),   // team 5 has 4; teams 1 and 2 can still get there
                new WinTarget(3, 3, 1));  // teams 1, 2 and 6 have 3; team 3 can get there
    }

    @Test
    void aOneRoundMeleeOnlyCountsTheWinners() {
        List<TeamRecord> records = List.of(new TeamRecord(1, 1, 0, 0), new TeamRecord(2, 0, 1, 0));

        assertThat(Standings.counter(records, 1)).containsExactly(new WinTarget(1, 1, 0));
    }
}
