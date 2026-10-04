package es.arrima.schedule.domain;

import java.util.List;
import java.util.random.RandomGenerator;

/** The whole court schedule: who plays whom (strategy) and on which court (CourtAssigner). */
public final class ScheduleGenerator {

    private final PairingStrategy pairingStrategy;

    public ScheduleGenerator(PairingStrategy pairingStrategy) {
        this.pairingStrategy = pairingStrategy;
    }

    public List<ScheduledRound> generate(List<Long> teamIds, int rounds, int courts, RandomGenerator random) {
        List<RoundPairings> pairings = pairingStrategy.pair(teamIds, rounds, random);
        return new CourtAssigner(courts, random).assign(pairings);
    }
}
