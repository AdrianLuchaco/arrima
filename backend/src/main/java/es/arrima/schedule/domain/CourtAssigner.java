package es.arrima.schedule.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Puts the matches of each round on the courts, round after round:
 * <ol>
 *   <li>if there are more matches than courts, the ones left over wait ("en espera"); teams that
 *       already waited in a previous round go first this time;</li>
 *   <li>"if possible, a team does not repeat a court": a maximum bipartite matching (Kuhn's
 *       augmenting paths) between matches and the courts none of their teams has played on;</li>
 *   <li>any match that could not avoid a repeat gets the free court with the fewest repeats.</li>
 * </ol>
 */
public final class CourtAssigner {

    private final int courts;
    private final RandomGenerator random;
    private final Map<Long, Set<Integer>> courtsPlayed = new HashMap<>();
    private final Map<Long, Integer> timesWaited = new HashMap<>();

    public CourtAssigner(int courts, RandomGenerator random) {
        this.courts = courts;
        this.random = random;
    }

    public List<ScheduledRound> assign(List<RoundPairings> rounds) {
        return rounds.stream().map(this::assignRound).toList();
    }

    private ScheduledRound assignRound(RoundPairings round) {
        List<Pairing> pairings = shuffled(round.pairings());
        // Stable sort after the shuffle: equal waiting times stay in random order.
        pairings.sort(Comparator.comparingInt(this::waitsOf).reversed());
        List<Pairing> onCourt = pairings.subList(0, Math.min(courts, pairings.size()));
        List<Pairing> waiting = pairings.subList(onCourt.size(), pairings.size());

        Map<Pairing, Integer> courtOf = assignWithoutRepeats(onCourt);
        assignRemainingWithFewestRepeats(onCourt, courtOf);

        List<ScheduledMatch> matches = new ArrayList<>();
        for (Pairing pairing : onCourt) {
            int court = courtOf.get(pairing);
            courtsPlayedBy(pairing.teamA()).add(court);
            courtsPlayedBy(pairing.teamB()).add(court);
            matches.add(new ScheduledMatch(pairing, court));
        }
        for (Pairing pairing : waiting) {
            timesWaited.merge(pairing.teamA(), 1, Integer::sum);
            timesWaited.merge(pairing.teamB(), 1, Integer::sum);
            matches.add(new ScheduledMatch(pairing, null));
        }
        return new ScheduledRound(round.number(), matches, round.byeTeam());
    }

    /** Kuhn's algorithm: each match tries a "new" court, displacing another match that has alternatives. */
    private Map<Pairing, Integer> assignWithoutRepeats(List<Pairing> pairings) {
        Map<Integer, Pairing> matchOfCourt = new HashMap<>();
        for (Pairing pairing : pairings) {
            tryAssign(pairing, matchOfCourt, new HashSet<>());
        }
        Map<Pairing, Integer> courtOf = new HashMap<>();
        matchOfCourt.forEach((court, pairing) -> courtOf.put(pairing, court));
        return courtOf;
    }

    private boolean tryAssign(Pairing pairing, Map<Integer, Pairing> matchOfCourt, Set<Integer> visited) {
        for (int court : shuffled(courtNumbers())) {
            if (repeats(pairing, court) > 0 || !visited.add(court)) {
                continue;
            }
            Pairing current = matchOfCourt.get(court);
            if (current == null || tryAssign(current, matchOfCourt, visited)) {
                matchOfCourt.put(court, pairing);
                return true;
            }
        }
        return false;
    }

    private void assignRemainingWithFewestRepeats(List<Pairing> pairings, Map<Pairing, Integer> courtOf) {
        Set<Integer> freeCourts = new HashSet<>(courtNumbers());
        freeCourts.removeAll(courtOf.values());
        for (Pairing pairing : pairings) {
            if (courtOf.containsKey(pairing)) {
                continue;
            }
            int best = shuffled(new ArrayList<>(freeCourts)).stream()
                    .min(Comparator.comparingInt(court -> repeats(pairing, court)))
                    .orElseThrow();
            freeCourts.remove(best);
            courtOf.put(pairing, best);
        }
    }

    private int repeats(Pairing pairing, int court) {
        return (courtsPlayedBy(pairing.teamA()).contains(court) ? 1 : 0)
                + (courtsPlayedBy(pairing.teamB()).contains(court) ? 1 : 0);
    }

    private int waitsOf(Pairing pairing) {
        return timesWaited.getOrDefault(pairing.teamA(), 0) + timesWaited.getOrDefault(pairing.teamB(), 0);
    }

    private Set<Integer> courtsPlayedBy(long team) {
        return courtsPlayed.computeIfAbsent(team, key -> new HashSet<>());
    }

    private List<Integer> courtNumbers() {
        List<Integer> numbers = new ArrayList<>();
        for (int court = 1; court <= courts; court++) {
            numbers.add(court);
        }
        return numbers;
    }

    private <T> List<T> shuffled(List<T> items) {
        List<T> copy = new ArrayList<>(items);
        for (int i = copy.size() - 1; i > 0; i--) {
            Collections.swap(copy, i, random.nextInt(i + 1));
        }
        return copy;
    }
}
