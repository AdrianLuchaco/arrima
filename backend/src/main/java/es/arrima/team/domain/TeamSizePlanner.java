package es.arrima.team.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Team sizes for a classic melee, including the club's "different team" rule when the players do not fit:
 * <ul>
 *   <li>doublettes, odd number of players: one triplette with the remaining three (25 → 11 × 2 + 1 × 3);</li>
 *   <li>triplettes, 2 left over: one doublette (26 → 8 × 3 + 1 × 2);</li>
 *   <li>triplettes, 1 left over: two doublettes instead of one triplette (25 → 7 × 3 + 2 × 2).</li>
 * </ul>
 */
public final class TeamSizePlanner {

    private TeamSizePlanner() {
    }

    public static TeamPlan plan(int players, int preferredSize) {
        List<Integer> sizes = switch (preferredSize) {
            case 2 -> doublettes(players);
            case 3 -> triplettes(players);
            default -> throw new IllegalArgumentException("Unsupported team size: " + preferredSize);
        };
        return new TeamPlan(players, preferredSize, sizes);
    }

    private static List<Integer> doublettes(int players) {
        if (players < 2) {
            return List.of();
        }
        if (players % 2 == 0) {
            return repeat(2, players / 2);
        }
        if (players < 3) {
            return List.of();
        }
        return concat(repeat(2, (players - 3) / 2), List.of(3));
    }

    private static List<Integer> triplettes(int players) {
        return switch (players % 3) {
            case 0 -> repeat(3, players / 3);
            case 2 -> concat(repeat(3, (players - 2) / 3), List.of(2));
            default -> players < 4 ? List.of() : concat(repeat(3, (players - 4) / 3), List.of(2, 2));
        };
    }

    private static List<Integer> repeat(int size, int times) {
        return Collections.nCopies(Math.max(0, times), size);
    }

    private static List<Integer> concat(List<Integer> first, List<Integer> second) {
        List<Integer> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }
}
