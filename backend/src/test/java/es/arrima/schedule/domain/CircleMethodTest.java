package es.arrima.schedule.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CircleMethodTest {

    @ParameterizedTest(name = "{0} teams")
    @ValueSource(ints = {2, 3, 6, 7, 20, 21, 60})
    void aFullRoundRobinMeetsEveryRule(int teamCount) {
        var teams = RandomPairingTest.teams(teamCount);
        int rounds = ScheduleLimits.maxRounds(teamCount);

        ScheduleInvariants.check(teams, CircleMethod.schedule(teams, rounds), rounds);
    }
}
