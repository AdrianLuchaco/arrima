package es.arrima.schedule.domain;

import java.util.List;

public record ScheduledRound(int number, List<ScheduledMatch> matches, Long byeTeam) {

    public ScheduledRound {
        matches = List.copyOf(matches);
    }
}
