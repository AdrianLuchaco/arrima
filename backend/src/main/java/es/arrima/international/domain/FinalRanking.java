package es.arrima.international.domain;

import es.arrima.international.domain.GroupRanking.GroupStanding;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** The prize positions once every group is decided: each group fills its positions with its order. */
public final class FinalRanking {

    private FinalRanking() {
    }

    public record RankedTeam(int position, long teamId) {
    }

    /**
     * @param groups    every group that holds prize positions (single teams included)
     * @param standings the standing of each group that had to play; single teams need none
     */
    public static List<RankedTeam> of(List<PlannedGroup> groups, Map<PlannedGroup, GroupStanding> standings) {
        List<RankedTeam> ranking = new ArrayList<>();
        for (PlannedGroup group : groups.stream().sorted(Comparator.comparingInt(PlannedGroup::bestPosition)).toList()) {
            List<Long> order = group.needsPlay() ? standings.get(group).order() : group.teamIds();
            for (int i = 0; i < order.size() && group.bestPosition() + i <= group.worstPosition(); i++) {
                ranking.add(new RankedTeam(group.bestPosition() + i, order.get(i)));
            }
        }
        return ranking;
    }
}
