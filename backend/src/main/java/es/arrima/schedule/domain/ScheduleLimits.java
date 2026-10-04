package es.arrima.schedule.domain;

/**
 * How many rounds are possible without two teams meeting twice:
 * <ul>
 *   <li>even number of teams: each team can face the other N−1, so N−1 rounds;</li>
 *   <li>odd number: one team rests each round and nobody rests twice, so N rounds
 *       (each team plays N−1 of them, against every other team once).</li>
 * </ul>
 */
public final class ScheduleLimits {

    private ScheduleLimits() {
    }

    public static int maxRounds(int teams) {
        if (teams < 2) {
            return 0;
        }
        return teams % 2 == 0 ? teams - 1 : teams;
    }

    static void check(int teams, int rounds) {
        if (rounds < 1 || rounds > maxRounds(teams)) {
            throw new IllegalArgumentException("%d rounds are not possible with %d teams".formatted(rounds, teams));
        }
    }
}
