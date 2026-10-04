package es.arrima.team.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * The classic draw, like the bingo drum: players are shuffled and dealt into teams. The order of the
 * team sizes is shuffled too, so who ends up in the odd-sized team, and its number, are also random.
 */
public final class RandomTeamFormation implements TeamFormationStrategy {

    @Override
    public List<List<Long>> formTeams(List<Long> playerIds, TeamPlan plan, RandomGenerator random) {
        if (playerIds.size() != plan.players()) {
            throw new IllegalArgumentException("The plan is for %d players, not %d".formatted(plan.players(), playerIds.size()));
        }
        List<Long> players = new ArrayList<>(playerIds);
        List<Integer> sizes = new ArrayList<>(plan.teamSizes());
        shuffle(players, random);
        shuffle(sizes, random);

        List<List<Long>> teams = new ArrayList<>();
        int next = 0;
        for (int size : sizes) {
            teams.add(List.copyOf(players.subList(next, next + size)));
            next += size;
        }
        return teams;
    }

    /** Fisher–Yates with the given generator, so tests can use a fixed seed. */
    static <T> void shuffle(List<T> list, RandomGenerator random) {
        for (int i = list.size() - 1; i > 0; i--) {
            Collections.swap(list, i, random.nextInt(i + 1));
        }
    }
}
