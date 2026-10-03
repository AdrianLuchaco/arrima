package es.arrima.club;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Points of la Internacional for each result of a ball. Each club scores its own way: the club keeps
 * its table and every melee stores a copy of it, so editing the profile never changes the history.
 */
@Embeddable
public record ScoringTable(
        @Column(name = "points_pointing_out") int pointingOut,
        @Column(name = "points_pointing_big_circle") int pointingBigCircle,
        @Column(name = "points_pointing_small_circle") int pointingSmallCircle,
        @Column(name = "points_pointing_near_jack") int pointingNearJack,
        @Column(name = "points_pointing_on_jack") int pointingOnJack,
        @Column(name = "points_shooting_miss") int shootingMiss,
        @Column(name = "points_shooting_hit") int shootingHit,
        @Column(name = "points_shooting_hit_out") int shootingHitOut,
        @Column(name = "points_shooting_carreau") int shootingCarreau) {

    public static final int MAX_POINTS = 99;

    /** The table from the club's paper sheet: 0-1-2-3-5 pointing, 0-1-2-5 shooting. */
    public static final ScoringTable DEFAULT = new ScoringTable(0, 1, 2, 3, 5, 0, 1, 2, 5);
}
