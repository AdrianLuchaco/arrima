package es.arrima.schedule.domain;

/**
 * What the courts and the teams allow. How many rounds are possible without two teams meeting twice:
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

    /**
     * Matches per round beyond the courts, played off court: 22 teams make 11 matches, so with
     * 10 courts one of them is off court every round (a team left over with an odd number rests).
     */
    public static int offCourtMatches(int teams, int courts) {
        return Math.max(0, teams / 2 - courts);
    }

    static void check(int teams, int rounds) {
        if (rounds < 1 || rounds > maxRounds(teams)) {
            throw new IllegalArgumentException("%d rounds are not possible with %d teams".formatted(rounds, teams));
        }
    }
}
