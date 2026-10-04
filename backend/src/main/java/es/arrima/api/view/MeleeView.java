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
        TeamPlan teamPlan,
        List<Team> teams,
        TeamIssues teamIssues,
        int maxRounds,
        List<Round> rounds,
        List<WinTarget> counter) {

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

    /** @param wins includes the bye, which counts as a win */
    public record Team(long id, int number, List<Long> memberIds, int wins, int losses, int pending) {
    }

    public record TeamIssues(List<Long> unassignedPlayers, List<Long> withdrawnMembers, List<Integer> teamsWithoutActivePlayers) {
    }

    /** @param byeTeamId the team that rests this round, or null */
    public record Round(int number, List<Match> matches, Long byeTeamId) {
    }

    /** @param courtNumber null while waiting for a court; @param winnerTeamId null until decided */
    public record Match(long id, long teamAId, long teamBId, Integer courtNumber, Long winnerTeamId) {
    }

    /** "4 wins: 2 teams, 5 more can reach it". */
    public record WinTarget(int wins, long reached, long canReach) {
    }
}
