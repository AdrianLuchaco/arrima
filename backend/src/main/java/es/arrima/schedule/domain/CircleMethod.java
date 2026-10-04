package es.arrima.schedule.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * The classic round-robin "circle method": teams sit around a circle, the first one stays still and
 * the rest rotate one place each round; each team plays whoever sits opposite. Over N−1 rounds every
 * pair meets exactly once. With an odd number of teams an empty seat is added: whoever sits opposite
 * it rests, and over N rounds each team rests exactly once.
 * <p>
 * Used as the guaranteed fallback of {@link RandomPairing}; the order of the teams it receives is
 * already shuffled, so the result is still random.
 */
final class CircleMethod {

    private CircleMethod() {
    }

    static List<RoundPairings> schedule(List<Long> teams, int rounds) {
        List<Long> circle = new ArrayList<>(teams);
        if (circle.size() % 2 == 1) {
            circle.add(null); // empty seat: its opponent rests
        }
        int seats = circle.size();
        List<RoundPairings> schedule = new ArrayList<>();
        for (int round = 1; round <= rounds; round++) {
            List<Pairing> pairings = new ArrayList<>();
            Long byeTeam = null;
            for (int i = 0; i < seats / 2; i++) {
                Long home = circle.get(i);
                Long away = circle.get(seats - 1 - i);
                if (home == null || away == null) {
                    byeTeam = home == null ? away : home;
                } else {
                    pairings.add(new Pairing(home, away));
                }
            }
            schedule.add(new RoundPairings(round, pairings, byeTeam));
            circle.add(1, circle.removeLast()); // rotate everyone except the first seat
        }
        return schedule;
    }
}
