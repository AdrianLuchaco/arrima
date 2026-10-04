package es.arrima.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class RandomPairingTest {

    private final RandomPairing strategy = new RandomPairing();

    /** Every team count from 6 to 60, with 1 to 5 rounds (the usual range), several seeds each. */
    static Stream<Arguments> usualMelees() {
        return IntStream.rangeClosed(6, 60).boxed()
                .flatMap(teams -> IntStream.rangeClosed(1, 5).mapToObj(rounds -> Arguments.of(teams, rounds)));
    }

    @ParameterizedTest(name = "{0} teams, {1} rounds")
    @MethodSource("usualMelees")
    void usualMeleesRespectEveryRule(int teamCount, int rounds) {
        List<Long> teams = teams(teamCount);
        for (int seed = 0; seed < 3; seed++) {
            ScheduleInvariants.check(teams, strategy.pair(teams, rounds, new Random(seed)), rounds);
        }
    }

    /** The hardest cases: every team must meet every other one (the fallback may be needed). */
    @ParameterizedTest(name = "full round robin with {0} teams")
    @CsvSource({"6", "7", "8", "9", "10", "12"})
    void fullRoundRobinsAreAlwaysFound(int teamCount) {
        List<Long> teams = teams(teamCount);
        int rounds = ScheduleLimits.maxRounds(teamCount);
        for (int seed = 0; seed < 10; seed++) {
            ScheduleInvariants.check(teams, strategy.pair(teams, rounds, new Random(seed)), rounds);
        }
    }

    @Test
    void pairingsAreRandom() {
        List<Long> teams = teams(20);
        Set<List<RoundPairings>> distinct = new HashSet<>();
        for (int seed = 0; seed < 10; seed++) {
            distinct.add(strategy.pair(teams, 3, new Random(seed)));
        }
        assertThat(distinct).hasSize(10);
    }

    @Test
    void whoRestsIsRandomToo() {
        List<Long> teams = teams(11);
        Set<Long> firstToRest = new HashSet<>();
        for (int seed = 0; seed < 40; seed++) {
            firstToRest.add(strategy.pair(teams, 3, new Random(seed)).getFirst().byeTeam());
        }
        assertThat(firstToRest).hasSizeGreaterThan(5);
    }

    @Test
    void tooManyRoundsForTheTeamsAreRejected() {
        assertThatThrownBy(() -> strategy.pair(teams(6), 6, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(ScheduleLimits.maxRounds(6)).isEqualTo(5);
        assertThat(ScheduleLimits.maxRounds(7)).isEqualTo(7);
    }

    static List<Long> teams(int count) {
        return LongStream.rangeClosed(1, count).boxed().toList();
    }
}
