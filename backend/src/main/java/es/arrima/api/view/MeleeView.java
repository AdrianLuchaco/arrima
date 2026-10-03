package es.arrima.api.view;

import es.arrima.club.ScoringTableDto;
import es.arrima.melee.MeleeFormat;
import es.arrima.melee.MeleeStatus;
import es.arrima.participant.ParticipantStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Everything a screen needs to show a melee, in one response. The admin and the public view get
 * the same structure: none of it is private (it is what used to be on the paper sheet).
 * Every change to the melee increases {@code revision}.
 */
public record MeleeView(
        long id,
        String publicCode,
        LocalDate playedOn,
        MeleeFormat format,
        int teamSize,
        MeleeStatus status,
        long revision,
        Settings settings,
        ScoringTableDto scoring,
        Club club,
        List<Participant> participants,
        TeamPlan teamPlan) {

    public record Settings(int courtCount, int roundsCount, int prizeCount) {
    }

    public record Club(String name, String logoUrl) {
    }

    public record Participant(long id, Integer listNumber, String name, ParticipantStatus status) {
    }

    /**
     * How the active players split into teams.
     *
     * @param teamsBySize e.g. {2: 11, 3: 1} for 25 players in doublettes
     */
    public record TeamPlan(int activePlayers, boolean fits, boolean playable, int teamCount, Map<Integer, Long> teamsBySize) {
    }
}
