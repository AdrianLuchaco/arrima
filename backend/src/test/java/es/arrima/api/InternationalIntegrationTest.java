package es.arrima.api;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.auth.SignupInvitationRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Phase 6 through the API. Four doublettes and three rounds: everyone meets everyone. Results are
 * chosen so that teams 1 and 2 end with 2 wins and teams 3 and 4 with 1; with 5 prizes both pairs
 * play: first the 1-win group (prizes 3rd-4th), then the 2-win group (1st-2nd).
 */
@IntegrationTest
class InternationalIntegrationTest {

    /** Winner of each pairing of team numbers ("1-2" means team 1 against team 2). */
    private static final Map<String, Integer> WINNERS = Map.of(
            "1-2", 1, "1-3", 3, "1-4", 1, "2-3", 2, "2-4", 2, "3-4", 4);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    private TestMelees melees;
    private RegisteredClub club;
    private long meleeId;
    private final Map<Integer, Long> teamIdOf = new HashMap<>();

    @BeforeEach
    void playTheMatches() {
        melees = new TestMelees(mvc);
        club = new TestClubs(mvc, invitations).register("Club Internacional");
        meleeId = melees.create(club, 2);
        melees.signUp(club, meleeId, 8);
        melees.send(club, "POST", "/api/melees/%d/teams/draw".formatted(meleeId), "{}");
        MvcTestResult schedule = melees.send(club, "POST", "/api/melees/%d/schedule/generate".formatted(meleeId), "{}");

        List<Map<String, Object>> teams = json(schedule, "$.teams");
        Map<Long, Integer> numberOf = new HashMap<>();
        teams.forEach(team -> {
            long id = ((Number) team.get("id")).longValue();
            int number = (Integer) team.get("number");
            numberOf.put(id, number);
            teamIdOf.put(number, id);
        });
        List<Map<String, Object>> matches = json(schedule, "$.rounds[*].matches[*]");
        for (Map<String, Object> match : matches) {
            int a = numberOf.get(((Number) match.get("teamAId")).longValue());
            int b = numberOf.get(((Number) match.get("teamBId")).longValue());
            int winner = WINNERS.get(Math.min(a, b) + "-" + Math.max(a, b));
            melees.send(club, "PUT", "/api/melees/%d/schedule/matchups/%s/winner".formatted(meleeId, match.get("id")),
                    "{\"winnerTeamId\":%d}".formatted(teamIdOf.get(winner)));
        }
    }

    @Test
    void theGroupWithFewerWinsPlaysFirstAndThePointerStarts() {
        MvcTestResult started = start(false);

        assertThat(started).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("INTERNATIONAL");
        assertThat(started).bodyJson().extractingPath("$.international.groups.length()").isEqualTo(2);
        assertThat(started).bodyJson().extractingPath("$.international.groups[0].wins").isEqualTo(1);
        assertThat(started).bodyJson().extractingPath("$.international.groups[0].bestPosition").isEqualTo(3);
        assertThat(started).bodyJson().extractingPath("$.international.turn").isEqualTo(Map.of(
                "groupId", json(started, "$.international.groups[0].id"),
                "roundId", json(started, "$.international.groups[0].rounds[0].id"),
                "teamId", teamIdOf.get(3).intValue(),
                "kind", "POINTING", "ballNumber", 1, "position", "POINTER"));
    }

    @Test
    void aTieForDifferentPrizesIsBrokenAndTheFinalRankingIsKnown() {
        MvcTestResult started = start(false);
        long lowRound = roundId(started, 0, 0);

        // Group of 1 win: teams 3 and 4 tie with 9 points each (3 + 3 + 3, 0 + 0 + 0).
        throwSix(lowRound, 3, "NEAR_JACK", "MISS");
        MvcTestResult tied = throwSix(lowRound, 4, "NEAR_JACK", "MISS");
        assertThat(tied).bodyJson().extractingPath("$.international.groups[0].rounds.length()").isEqualTo(2);
        long tieBreak = roundId(tied, 0, 1);

        throwSix(tieBreak, 3, "OUT", "MISS");
        MvcTestResult broken = throwSix(tieBreak, 4, "ON_JACK", "MISS");
        assertThat(broken).bodyJson().extractingPath("$.international.groups[0].status").isEqualTo("FINISHED");
        assertThat(broken).bodyJson().extractingPath("$.international.turn.groupId")
                .isEqualTo(json(broken, "$.international.groups[1].id"));

        long topRound = roundId(broken, 1, 0);
        throwSix(topRound, 1, "SMALL_CIRCLE", "HIT");     // 2*3 + 1*3 = 9
        MvcTestResult done = throwSix(topRound, 2, "ON_JACK", "CARREAU"); // 15 + 15 = 30

        assertThat(done).bodyJson().extractingPath("$.international.complete").isEqualTo(true);
        assertThat(done).bodyJson().extractingPath("$.international.finalRanking").isEqualTo(List.of(
                Map.of("position", 1, "teamId", teamIdOf.get(2).intValue(), "points", 30, "tieBreak", false),
                Map.of("position", 2, "teamId", teamIdOf.get(1).intValue(), "points", 9, "tieBreak", false),
                Map.of("position", 3, "teamId", teamIdOf.get(4).intValue(), "points", 9, "tieBreak", true),
                Map.of("position", 4, "teamId", teamIdOf.get(3).intValue(), "points", 9, "tieBreak", true)));
    }

    @Test
    void aCorrectionThatUndoesAPlayedTieBreakIsFlagged() {
        MvcTestResult started = start(false);
        long lowRound = roundId(started, 0, 0);
        throwSix(lowRound, 3, "NEAR_JACK", "MISS");
        long tieBreak = roundId(throwSix(lowRound, 4, "NEAR_JACK", "MISS"), 0, 1);
        throwSix(tieBreak, 3, "OUT", "MISS");
        throwSix(tieBreak, 4, "ON_JACK", "MISS");

        // Team 3 complains: its first ball was a "chupete", not "a menos de 10 cm".
        MvcTestResult corrected = throwBall(lowRound, 3, "POINTING", 1, "ON_JACK");

        assertThat(corrected).bodyJson().extractingPath("$.international.groups[0].obsoletePlayed").isEqualTo(true);
        assertThat(corrected).bodyJson().extractingPath("$.international.groups[0].order")
                .isEqualTo(List.of(teamIdOf.get(3).intValue(), teamIdOf.get(4).intValue()));
        assertThat(corrected).bodyJson().extractingPath("$.international.groups[0].rounds[0].teams[0].balls[0]")
                .isEqualTo(Map.of("kind", "POINTING", "ballNumber", 1, "position", "POINTER", "outcome", "ON_JACK",
                        "points", 5, "corrected", true));
    }

    @Test
    void theSameBallSentTwiceIsStoredOnce() {
        long lowRound = roundId(start(false), 0, 0);

        throwBall(lowRound, 3, "POINTING", 1, "BIG_CIRCLE");
        MvcTestResult again = throwBall(lowRound, 3, "POINTING", 1, "BIG_CIRCLE");

        assertThat(again).bodyJson().extractingPath("$.international.groups[0].rounds[0].teams[0].balls.length()").isEqualTo(1);
        assertThat(again).bodyJson().extractingPath("$.international.groups[0].rounds[0].teams[0].balls[0].corrected").isEqualTo(false);
    }

    @Test
    void anOutcomeOfTheOtherKindIsRejected() {
        long lowRound = roundId(start(false), 0, 0);

        assertThat(throwBall(lowRound, 3, "POINTING", 1, "CARREAU"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.outcome").isEqualTo("WrongKind");
    }

    @Test
    void itCannotStartWithResultsMissing() {
        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null); // to TEAMS
        long meleeWithPending = meleeId;
        MvcTestResult regenerated = melees.send(club, "POST", "/api/melees/%d/schedule/generate".formatted(meleeWithPending),
                "{\"confirmLosses\":true}");
        assertThat(regenerated).bodyJson().extractingPath("$.status").isEqualTo("MATCHES");

        assertThat(start(false))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("RESULTS_MISSING");
    }

    @Test
    void goingBackAndForwardKeepsTheGroupsThatDidNotChange() {
        long lowRound = roundId(start(false), 0, 0);
        throwBall(lowRound, 3, "POINTING", 1, "BIG_CIRCLE");

        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null);
        MvcTestResult restarted = start(false);

        assertThat(restarted).hasStatusOk().bodyJson()
                .extractingPath("$.international.groups[0].rounds[0].teams[0].balls.length()").isEqualTo(1);
    }

    @Test
    void changingAResultThatChangesTheGroupsAsksBeforeLosingBalls() {
        long lowRound = roundId(start(false), 0, 0);
        throwBall(lowRound, 3, "POINTING", 1, "BIG_CIRCLE");
        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null);

        // Team 3 now beats team 2 instead: wins become 1:1, 2:1, 3:2, 4:2 — new groups.
        MvcTestResult view = melees.get(club, meleeId);
        List<Map<String, Object>> matches = json(view, "$.rounds[*].matches[*]");
        Map<String, Object> twoVsThree = matches.stream().filter(match -> {
            long a = ((Number) match.get("teamAId")).longValue();
            long b = ((Number) match.get("teamBId")).longValue();
            return (a == teamIdOf.get(2) && b == teamIdOf.get(3)) || (a == teamIdOf.get(3) && b == teamIdOf.get(2));
        }).findFirst().orElseThrow();
        melees.send(club, "PUT", "/api/melees/%d/schedule/matchups/%s/winner".formatted(meleeId, twoVsThree.get("id")),
                "{\"winnerTeamId\":%d}".formatted(teamIdOf.get(3)));

        assertThat(start(false))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.losses.ballThrows").isEqualTo(1);
        assertThat(start(true)).hasStatusOk();
    }

    @Test
    void spectatorsSeeTheInternacionalOnlyOnceItStarts() {
        String code = json(melees.get(club, meleeId), "$.publicCode");
        assertThat(mvc.get().uri("/api/public/melees/" + code)).bodyJson().extractingPath("$.international").isNull();

        start(false);

        assertThat(mvc.get().uri("/api/public/melees/" + code)).bodyJson().extractingPath("$.international.groups.length()").isEqualTo(2);
    }

    private MvcTestResult start(boolean confirmLosses) {
        return melees.send(club, "POST", "/api/melees/%d/international/start".formatted(meleeId),
                "{\"confirmLosses\":%s}".formatted(confirmLosses));
    }

    /** Three pointing balls with one outcome and three shooting balls with another. */
    private MvcTestResult throwSix(long roundId, int teamNumber, String pointing, String shooting) {
        MvcTestResult last = null;
        for (int ball = 1; ball <= 3; ball++) {
            last = throwBall(roundId, teamNumber, "POINTING", ball, pointing);
        }
        for (int ball = 1; ball <= 3; ball++) {
            last = throwBall(roundId, teamNumber, "SHOOTING", ball, shooting);
        }
        return last;
    }

    private MvcTestResult throwBall(long roundId, int teamNumber, String kind, int ball, String outcome) {
        return melees.send(club, "PUT", "/api/melees/%d/international/rounds/%d/teams/%d/throws/%s/%d"
                .formatted(meleeId, roundId, teamIdOf.get(teamNumber), kind, ball), "{\"outcome\":\"%s\"}".formatted(outcome));
    }

    private static long roundId(MvcTestResult result, int group, int round) {
        return ((Number) json(result, "$.international.groups[%d].rounds[%d].id".formatted(group, round))).longValue();
    }
}
