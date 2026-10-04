package es.arrima.schedule.domain;

import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Decides who plays whom in every round, all at the start (as on the paper sheet), independently of
 * results. Other formats can plug in their own rules.
 */
public interface PairingStrategy {

    /** @param rounds must be between 1 and {@link ScheduleLimits#maxRounds(int)} */
    List<RoundPairings> pair(List<Long> teamIds, int rounds, RandomGenerator random);
}
