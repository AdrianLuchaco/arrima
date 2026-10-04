package es.arrima.api.view;

import es.arrima.club.ScoringTableDto;
import es.arrima.international.domain.PlayerPosition;
import es.arrima.international.domain.ThrowKind;
import es.arrima.international.domain.ThrowOutcome;
import es.arrima.melee.MeleeFormat;
import es.arrima.melee.MeleeStatus;
import es.arrima.participant.ParticipantStatus;
import es.arrima.participant.PaymentStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Everything a screen needs to show a melee, in one response. The admin and the public view get
 * the same structure, and almost none of it is private (it is what used to be on the paper sheet).
 * The exception is who has paid: {@code payments} and each participant's {@code paymentStatus} are
 * only filled in for the admin. Every change to the melee increases {@code revision}.
 *
 * @param payments null without an entry fee, and always for spectators
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
        List<WinTarget> counter,
        International international,
        List<PrizeView> prizes,
        Payments payments) {

    public record Settings(int courtCount, int roundsCount, int prizeCount, int entryFeeCents) {
    }

    public record Club(String name, String logoUrl) {
    }

    /**
     * @param paymentStatus only for the admin, and only if the melee has an entry fee; otherwise null
     * @param notPlaying    after the draw: signed up and here, but not a player (did not pay). Shown
     *                      to everyone as "No juega", without saying why
     */
    public record Participant(long id, Integer listNumber, String name, ParticipantStatus status,
            PaymentStatus paymentStatus, boolean notPlaying) {
    }

    /**
     * How the players (see Participant#plays) split into teams.
     *
     * @param teamsBySize e.g. {2: 11, 3: 1} for 25 players in doublettes
     */
    public record TeamPlan(int players, boolean fits, boolean playable, int teamCount, Map<Integer, Long> teamsBySize) {
    }

    /** "34 de 35 han pagado · 170 €" (see PaymentSummary). */
    public record Payments(int expected, int paid, int unpaid, int unmarked, int entryFeeCents, long collectedCents) {
    }

    /** @param wins includes the bye, which counts as a win */
    public record Team(long id, int number, List<Long> memberIds, int wins, int losses, int pending) {
    }

    public record TeamIssues(List<Long> unassignedPlayers, List<Long> membersNotPlaying, List<Integer> teamsWithoutPlayers) {
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

    /**
     * La Internacional; null until it starts.
     *
     * @param assured      teams whose prize needs no playing (alone in their group), with its position
     * @param turn         the next ball to throw, or null
     * @param finalRanking prize positions once everything is decided, otherwise empty
     */
    public record International(List<IntlGroup> groups, List<Ranked> assured, Turn turn, List<Ranked> finalRanking,
            boolean complete) {
    }

    /**
     * @param order          ranking so far, best first
     * @param obsoletePlayed a tie-break already played no longer matches any tie after a correction
     */
    public record IntlGroup(long id, int playOrder, int wins, int bestPosition, int worstPosition, String status,
            List<Long> order, boolean obsoletePlayed, List<IntlRound> rounds) {
    }

    /** Round 1 has the whole group; rounds 2+ are tie-breaks among tied teams. */
    public record IntlRound(long id, int number, boolean obsolete, boolean complete, List<IntlTeam> teams) {
    }

    public record IntlTeam(long teamId, int playOrder, int points, List<Ball> balls) {
    }

    public record Ball(ThrowKind kind, int ballNumber, PlayerPosition position, ThrowOutcome outcome, int points,
            boolean corrected) {
    }

    public record Turn(long groupId, long roundId, long teamId, ThrowKind kind, int ballNumber, PlayerPosition position) {
    }

    /**
     * @param points   points of the group's first round, null if the team did not need to play
     * @param tieBreak the team's place was decided in a tie-break round
     */
    public record Ranked(int position, long teamId, Integer points, boolean tieBreak) {
    }

    /**
     * @param points  la Internacional's points (first round), null if the team did not need to play
     * @param awarded already handed out in the ceremony
     */
    public record PrizeView(long id, int position, long teamId, Integer points, boolean awarded, List<Photo> photos) {
    }

    public record Photo(long id, String url) {
    }
}
