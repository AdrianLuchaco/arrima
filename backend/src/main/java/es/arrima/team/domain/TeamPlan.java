package es.arrima.team.domain;

import java.util.List;

/**
 * How many teams of each size the active players make.
 *
 * @param players       active players
 * @param preferredSize size chosen for the melee (2 doublettes, 3 triplettes)
 * @param teamSizes     size of every team, preferred size first
 */
public record TeamPlan(int players, int preferredSize, List<Integer> teamSizes) {

    public TeamPlan {
        teamSizes = List.copyOf(teamSizes);
    }

    public int teamCount() {
        return teamSizes.size();
    }

    /** At least two teams are needed to play a match. */
    public boolean isPlayable() {
        return teamCount() >= 2;
    }

    /** Every team has the chosen size: the players "fit". */
    public boolean fits() {
        return isPlayable() && teamSizes.stream().allMatch(size -> size == preferredSize);
    }

    public long countOfSize(int size) {
        return teamSizes.stream().filter(teamSize -> teamSize == size).count();
    }
}
