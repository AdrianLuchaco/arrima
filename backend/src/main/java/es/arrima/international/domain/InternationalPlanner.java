package es.arrima.international.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Who plays la Internacional and for which prizes. With P prizes, teams are grouped by wins, from the
 * most wins down:
 * <ul>
 *   <li>a group that fits entirely in the prizes left has them secured and plays only to be ordered;</li>
 *   <li>the group that crosses the cut plays for the prizes left;</li>
 *   <li>groups below the cut do not play;</li>
 *   <li>a group of a single team does not need to play.</li>
 * </ul>
 * Groups play starting with the one with the fewest wins that still competes for a prize.
 */
public final class InternationalPlanner {

    private InternationalPlanner() {
    }

    /** Every group that holds prize positions, from the first prize down (single teams included). */
    public static List<PlannedGroup> plan(List<TeamWins> teams, int prizeCount) {
        Map<Integer, List<TeamWins>> byWins = new TreeMap<>(Comparator.reverseOrder());
        teams.forEach(team -> byWins.computeIfAbsent(team.wins(), wins -> new ArrayList<>()).add(team));

        List<PlannedGroup> groups = new ArrayList<>();
        int nextPosition = 1;
        int prizesLeft = prizeCount;
        for (Map.Entry<Integer, List<TeamWins>> entry : byWins.entrySet()) {
            if (prizesLeft == 0) {
                break;
            }
            List<Long> teamIds = entry.getValue().stream()
                    .sorted(Comparator.comparingInt(TeamWins::teamNumber))
                    .map(TeamWins::teamId)
                    .toList();
            int prizesForGroup = Math.min(teamIds.size(), prizesLeft);
            groups.add(new PlannedGroup(entry.getKey(), teamIds, nextPosition, nextPosition + prizesForGroup - 1));
            nextPosition += prizesForGroup;
            prizesLeft -= prizesForGroup;
        }
        return groups;
    }

    /** Only the groups that must play, in playing order: the fewest wins first. */
    public static List<PlannedGroup> playingOrder(List<PlannedGroup> groups) {
        return groups.stream()
                .filter(PlannedGroup::needsPlay)
                .sorted(Comparator.comparingInt(PlannedGroup::wins))
                .toList();
    }
}
