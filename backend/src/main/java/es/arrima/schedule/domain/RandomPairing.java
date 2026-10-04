package es.arrima.schedule.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Random pairings for the classic melee:
 * <ul>
 *   <li>no two teams meet twice;</li>
 *   <li>with an odd number of teams, one team rests each round, chosen at random, and no team rests twice.</li>
 * </ul>
 * Each round is built by backtracking: pair the team with the fewest possible opponents left with a
 * random one of them, and undo when stuck. If the random search does not succeed within a budget
 * (only plausible with many rounds and few teams), the round-robin "circle method" on a shuffled
 * order gives a valid schedule every time.
 */
public final class RandomPairing implements PairingStrategy {

    private static final int ATTEMPTS = 100;
    private static final int STEP_BUDGET_PER_ROUND = 5_000;

    @Override
    public List<RoundPairings> pair(List<Long> teamIds, int rounds, RandomGenerator random) {
        ScheduleLimits.check(teamIds.size(), rounds);
        for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
            List<RoundPairings> schedule = tryRandomSchedule(teamIds, rounds, random);
            if (schedule != null) {
                return schedule;
            }
        }
        return CircleMethod.schedule(shuffled(teamIds, random), rounds);
    }

    private static List<RoundPairings> tryRandomSchedule(List<Long> teamIds, int rounds, RandomGenerator random) {
        boolean oddTeams = teamIds.size() % 2 == 1;
        List<Long> restingOrder = shuffled(teamIds, random); // the first R teams rest, one per round
        Map<Long, Set<Long>> opponents = new HashMap<>();
        teamIds.forEach(team -> opponents.put(team, new HashSet<>()));

        List<RoundPairings> schedule = new ArrayList<>();
        for (int round = 1; round <= rounds; round++) {
            Long byeTeam = oddTeams ? restingOrder.get(round - 1) : null;
            List<Long> playing = new ArrayList<>(teamIds);
            playing.remove(byeTeam);

            List<Pairing> pairings = new RoundSearch(opponents, random).pairAll(playing);
            if (pairings == null) {
                return null;
            }
            for (Pairing pairing : pairings) {
                opponents.get(pairing.teamA()).add(pairing.teamB());
                opponents.get(pairing.teamB()).add(pairing.teamA());
            }
            schedule.add(new RoundPairings(round, pairings, byeTeam));
        }
        return schedule;
    }

    private static List<Long> shuffled(List<Long> teams, RandomGenerator random) {
        List<Long> copy = new ArrayList<>(teams);
        for (int i = copy.size() - 1; i > 0; i--) {
            Collections.swap(copy, i, random.nextInt(i + 1));
        }
        return copy;
    }

    /** Backtracking search for one round in which every playing team meets a new opponent. */
    private static final class RoundSearch {

        private final Map<Long, Set<Long>> previousOpponents;
        private final RandomGenerator random;
        private int steps;

        RoundSearch(Map<Long, Set<Long>> previousOpponents, RandomGenerator random) {
            this.previousOpponents = previousOpponents;
            this.random = random;
        }

        List<Pairing> pairAll(List<Long> teams) {
            List<Pairing> pairings = new ArrayList<>();
            return search(new ArrayList<>(teams), pairings) ? pairings : null;
        }

        private boolean search(List<Long> unpaired, List<Pairing> pairings) {
            if (unpaired.isEmpty()) {
                return true;
            }
            if (++steps > STEP_BUDGET_PER_ROUND) {
                return false;
            }
            // The most constrained team first: it is the one most likely to be left without an opponent.
            Long team = null;
            List<Long> candidates = null;
            for (Long candidate : unpaired) {
                List<Long> options = newOpponentsFor(candidate, unpaired);
                if (candidates == null || options.size() < candidates.size()) {
                    team = candidate;
                    candidates = options;
                }
            }
            for (Long opponent : shuffled(candidates, random)) {
                unpaired.remove(team);
                unpaired.remove(opponent);
                pairings.add(new Pairing(team, opponent));
                if (search(unpaired, pairings)) {
                    return true;
                }
                pairings.removeLast();
                unpaired.add(team);
                unpaired.add(opponent);
            }
            return false;
        }

        private List<Long> newOpponentsFor(Long team, List<Long> unpaired) {
            List<Long> options = new ArrayList<>();
            for (Long other : unpaired) {
                if (!other.equals(team) && !previousOpponents.get(team).contains(other)) {
                    options.add(other);
                }
            }
            return options;
        }
    }
}
