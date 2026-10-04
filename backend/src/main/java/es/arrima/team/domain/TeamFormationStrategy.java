package es.arrima.team.domain;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * How players are grouped into teams. The classic melee draws at random; future formats (for example
 * "by roles", pairing a pointer with a shooter) will be other implementations.
 */
public interface TeamFormationStrategy {

    /**
     * @param playerIds active players
     * @param plan      sizes of the teams to form (their sum equals the number of players)
     * @return the members of each team; the list order is the team number (1, 2, 3...)
     */
    List<List<Long>> formTeams(List<Long> playerIds, TeamPlan plan, RandomGenerator random);
}
