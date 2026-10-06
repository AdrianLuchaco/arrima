package es.arrima.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CourtAssignerTest {

    @Test
    void eachCourtHoldsOneMatchPerRoundAndExtraMatchesWait() {
        List<ScheduledRound> schedule = generate(40, 4, 15, 1); // 20 matches per round, 15 courts

        for (ScheduledRound round : schedule) {
            List<Integer> courts = round.matches().stream().map(ScheduledMatch::court).filter(Objects::nonNull).toList();
            assertThat(courts).doesNotHaveDuplicates().allMatch(court -> court >= 1 && court <= 15).hasSize(15);
            assertThat(round.matches().stream().filter(match -> match.court() == null)).hasSize(5);
        }
    }

    @Test
    void withMoreCourtsThanMatchesTheFirstCourtsArePlayed() {
        // 10 teams, 10 courts: 5 matches a round, always on courts 1 to 5; 6 to 10 stay free.
        for (int seed = 0; seed < 20; seed++) {
            for (ScheduledRound round : generate(10, 3, 10, seed)) {
                assertThat(round.matches()).extracting(ScheduledMatch::court)
                        .as("seed %d, round %d", seed, round.number()).containsExactlyInAnyOrder(1, 2, 3, 4, 5);
            }
        }
    }

    @Test
    void withATeamRestingTheFirstCourtsAreStillTheOnesPlayed() {
        // 9 teams: one rests each round and the other 8 play 4 matches, on courts 1 to 4 of 12.
        for (int seed = 0; seed < 20; seed++) {
            for (ScheduledRound round : generate(9, 3, 12, seed)) {
                assertThat(round.byeTeam()).isNotNull();
                assertThat(round.matches()).extracting(ScheduledMatch::court)
                        .as("seed %d, round %d", seed, round.number()).containsExactlyInAnyOrder(1, 2, 3, 4);
            }
        }
    }

    @Test
    void whoWaitedPlaysFirstInTheNextRound() {
        List<RoundPairings> rounds = List.of(
                new RoundPairings(1, List.of(new Pairing(1, 2), new Pairing(3, 4)), null),
                new RoundPairings(2, List.of(new Pairing(1, 2), new Pairing(3, 4)), null));

        for (int seed = 0; seed < 20; seed++) {
            List<ScheduledRound> schedule = new CourtAssigner(1, new Random(seed)).assign(rounds);

            Pairing waitedInRoundOne = schedule.get(0).matches().stream()
                    .filter(match -> match.court() == null).findFirst().orElseThrow().pairing();
            assertThat(schedule.get(1).matches())
                    .filteredOn(match -> match.pairing().equals(waitedInRoundOne))
                    .extracting(ScheduledMatch::court).containsExactly(1);
        }
    }

    @Test
    void waitingIsSpreadOut() {
        // 40 teams, 15 courts: 10 teams wait each round. The pairings are fixed in advance, so a
        // second wait cannot always be avoided, but nobody should wait a third time.
        for (int seed = 0; seed < 10; seed++) {
            Map<Long, Integer> waits = new HashMap<>();
            generate(40, 4, 15, seed).forEach(round -> round.matches().stream()
                    .filter(match -> match.court() == null)
                    .forEach(match -> {
                        waits.merge(match.pairing().teamA(), 1, Integer::sum);
                        waits.merge(match.pairing().teamB(), 1, Integer::sum);
                    }));

            assertThat(waits.values()).allMatch(times -> times <= 2);
        }
    }

    @Test
    void withEnoughCourtsNoTeamRepeatsACourt() {
        for (int seed = 0; seed < 20; seed++) {
            List<ScheduledRound> schedule = generate(20, 4, 10, seed);

            Map<Long, Set<Integer>> courtsOfTeam = new HashMap<>();
            for (ScheduledRound round : schedule) {
                for (ScheduledMatch match : round.matches()) {
                    assertThat(courtsOfTeam.computeIfAbsent(match.pairing().teamA(), team -> new HashSet<>()).add(match.court()))
                            .as("seed %d: team %d repeats court %d", seed, match.pairing().teamA(), match.court()).isTrue();
                    assertThat(courtsOfTeam.computeIfAbsent(match.pairing().teamB(), team -> new HashSet<>()).add(match.court()))
                            .as("seed %d: team %d repeats court %d", seed, match.pairing().teamB(), match.court()).isTrue();
                }
            }
        }
    }

    @Test
    void theGeneratorKeepsThePairingRules() {
        List<Long> teams = RandomPairingTest.teams(25);
        List<ScheduledRound> schedule = new ScheduleGenerator(new RandomPairing()).generate(teams, 4, 8, new Random(3));

        List<RoundPairings> pairings = schedule.stream()
                .map(round -> new RoundPairings(round.number(),
                        round.matches().stream().map(ScheduledMatch::pairing).toList(), round.byeTeam()))
                .toList();
        ScheduleInvariants.check(teams, pairings, 4);
    }

    private static List<ScheduledRound> generate(int teams, int rounds, int courts, long seed) {
        return new ScheduleGenerator(new RandomPairing())
                .generate(RandomPairingTest.teams(teams), rounds, courts, new Random(seed));
    }
}
