package es.arrima.international.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Orders the teams of a group from its rounds, and says whether another round is needed.
 * <p>
 * The group starts as one "tier" of tied teams. Each round is played by exactly the teams of one
 * tier and splits it by points (more points, better position); equal points stay tied. A tie needs
 * another round, among those teams only, when it decides a prize: when the tied teams occupy at least
 * one prize position. Ties entirely outside the prizes are not broken. Round 1 is simply the round
 * that splits the initial tier (the whole group).
 * <p>
 * Tie-break points only order the tied teams among themselves. If a correction changes an earlier
 * round, a tie-break may no longer match any tie: that round is "obsolete" and is ignored.
 */
public final class GroupRanking {

    private GroupRanking() {
    }

    /**
     * @param teamIds  teams of the group in team-number order
     * @param rounds   the group's rounds in the order they were created
     */
    public static GroupStanding rank(List<Long> teamIds, int bestPosition, int worstPosition, List<RoundResult> rounds) {
        List<List<Long>> tiers = new ArrayList<>();
        tiers.add(List.copyOf(teamIds));
        Set<Long> obsoleteRounds = new HashSet<>();
        Set<Set<Long>> tiersBeingPlayed = new HashSet<>();

        for (RoundResult round : rounds) {
            int tier = indexOfTier(tiers, round.teamIds());
            if (tier < 0) {
                obsoleteRounds.add(round.roundId());
            } else if (!round.complete()) {
                tiersBeingPlayed.add(round.teamIds());
            } else {
                tiers.addAll(tier, splitByPoints(tiers.remove(tier), round.points()));
            }
        }

        Set<Long> tieToBreak = Set.of();
        int position = bestPosition;
        for (List<Long> tier : tiers) {
            boolean decidesAPrize = tier.size() > 1 && position <= worstPosition;
            if (decidesAPrize && !tiersBeingPlayed.contains(Set.copyOf(tier))) {
                tieToBreak = Set.copyOf(tier);
                break;
            }
            position += tier.size();
        }
        boolean complete = tiersBeingPlayed.isEmpty() && tieToBreak.isEmpty();
        return new GroupStanding(tiers, tieToBreak, obsoleteRounds, complete);
    }

    private static int indexOfTier(List<List<Long>> tiers, Set<Long> teamIds) {
        for (int i = 0; i < tiers.size(); i++) {
            if (Set.copyOf(tiers.get(i)).equals(teamIds)) {
                return i;
            }
        }
        return -1;
    }

    /** Most points first; teams with equal points stay together, in their previous order. */
    private static List<List<Long>> splitByPoints(List<Long> tier, Map<Long, Integer> points) {
        Map<Integer, List<Long>> byPoints = new TreeMap<>(Comparator.reverseOrder());
        for (Long team : tier) {
            byPoints.computeIfAbsent(points.getOrDefault(team, 0), total -> new ArrayList<>()).add(team);
        }
        return new ArrayList<>(byPoints.values());
    }

    /**
     * @param teamIds  the teams that play the round
     * @param points   each team's total in this round
     * @param complete every team has thrown its six balls
     */
    public record RoundResult(long roundId, Set<Long> teamIds, Map<Long, Integer> points, boolean complete) {
    }

    /**
     * @param tiers          the order so far: each inner list holds teams still tied with each other
     * @param tieToBreak     teams that must play a new round now (empty if none)
     * @param obsoleteRounds rounds that no longer match any tie (after a correction)
     * @param complete       the order of every prize in the group is decided
     */
    public record GroupStanding(List<List<Long>> tiers, Set<Long> tieToBreak, Set<Long> obsoleteRounds, boolean complete) {

        public List<Long> order() {
            return tiers.stream().flatMap(List::stream).toList();
        }
    }
}
