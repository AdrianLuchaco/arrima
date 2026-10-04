package es.arrima.international.domain;

import java.util.Map;

/** The points each outcome is worth in a melee (the club's table, copied into the melee). */
public record PointsTable(Map<ThrowOutcome, Integer> points) {

    public PointsTable {
        points = Map.copyOf(points);
        for (ThrowOutcome outcome : ThrowOutcome.values()) {
            if (!points.containsKey(outcome)) {
                throw new IllegalArgumentException("No points for " + outcome);
            }
        }
    }

    public int pointsFor(ThrowOutcome outcome) {
        return points.get(outcome);
    }
}
