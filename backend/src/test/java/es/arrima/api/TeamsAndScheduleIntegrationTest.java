package es.arrima.api;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.auth.SignupInvitationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Phase 4: teams, court schedule, results and the counter, through the API. */
@IntegrationTest
class TeamsAndScheduleIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    private TestClubs clubs;
    private TestMelees melees;
    private RegisteredClub club;

    @BeforeEach
    void setUp() {
        clubs = new TestClubs(mvc, invitations);
        melees = new TestMelees(mvc);
        club = clubs.register("Club Petanca");
    }

    @Test
    void fortyPlayersMakeTwentyDoublettes() {
        long meleeId = meleeWithPlayers(40);

        MvcTestResult result = draw(meleeId, false, false);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("TEAMS");
        List<Integer> numbers = json(result, "$.teams[*].number");
        List<Integer> sizes = json(result, "$.teams[*].memberIds.length()");
        assertThat(numbers).hasSize(20).startsWith(1, 2, 3).endsWith(20);
        assertThat(sizes).containsOnly(2);
    }

    @Test
    void whenThePlayersDoNotFitTheDifferentTeamMustBeChosen() {
        long meleeId = meleeWithPlayers(25);

        assertThat(draw(meleeId, false, false))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("TEAMS_DO_NOT_FIT");

        List<Integer> sizes = json(draw(meleeId, true, false), "$.teams[*].memberIds.length()");
        assertThat(sizes).hasSize(12).containsOnly(2, 3);
        assertThat(sizes.stream().filter(size -> size == 3)).hasSize(1);
    }

    @Test
    void withdrawnPlayersAreNotDrawn() {
        long meleeId = meleeWithPlayers(5);
        long withdrawn = id(melees.get(club, meleeId), "$.participants[4].id");
        melees.send(club, "POST", "/api/melees/%d/participants/%d/withdraw".formatted(meleeId, withdrawn), null);

        MvcTestResult result = draw(meleeId, false, false);

        List<Integer> members = json(result, "$.teams[*].memberIds[*]");
        assertThat(members).hasSize(4).doesNotContain((int) withdrawn);
    }

    @Test
    void twoPlayersOfDifferentTeamsCanBeSwapped() {
        long meleeId = meleeWithPlayers(4);
        MvcTestResult drawn = draw(meleeId, false, false);
        long first = id(drawn, "$.teams[0].memberIds[0]");
        long second = id(drawn, "$.teams[1].memberIds[0]");

        MvcTestResult swapped = melees.send(club, "POST", "/api/melees/%d/teams/swap".formatted(meleeId),
                "{\"firstPlayerId\":%d,\"secondPlayerId\":%d}".formatted(first, second));

        List<Integer> teamOne = json(swapped, "$.teams[0].memberIds");
        List<Integer> teamTwo = json(swapped, "$.teams[1].memberIds");
        assertThat(teamOne).contains((int) second).doesNotContain((int) first);
        assertThat(teamTwo).contains((int) first).doesNotContain((int) second);
    }

    @Test
    void theScheduleFixesEveryRoundAtTheStartAndAnOddNumberOfTeamsHasByes() {
        long meleeId = meleeWithPlayers(26); // 13 doublettes
        draw(meleeId, false, false);

        MvcTestResult result = generate(meleeId, false);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("MATCHES");
        assertThat(result).bodyJson().extractingPath("$.rounds.length()").isEqualTo(3);
        List<Integer> matchesPerRound = json(result, "$.rounds[*].matches.length()");
        List<Integer> byes = json(result, "$.rounds[*].byeTeamId");
        assertThat(matchesPerRound).containsExactly(6, 6, 6);
        assertThat(byes).hasSize(3).doesNotContainNull().doesNotHaveDuplicates();
        // The bye counts as a win from the start.
        List<Integer> wins = json(result, "$.teams[*].wins");
        assertThat(wins.stream().mapToInt(Integer::intValue).sum()).isEqualTo(3);
    }

    @Test
    void oneTapRecordsTheWinnerAndTheCounterFollows() {
        long meleeId = meleeWithPlayers(8); // 4 doublettes, 3 rounds
        draw(meleeId, false, false);
        MvcTestResult schedule = generate(meleeId, false);
        long matchId = id(schedule, "$.rounds[0].matches[0].id");
        long winner = id(schedule, "$.rounds[0].matches[0].teamAId");

        MvcTestResult result = setWinner(meleeId, matchId, winner);

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.rounds[0].matches[0].winnerTeamId").isEqualTo((int) winner);
        assertThat(result).bodyJson().extractingPath("$.counter").isEqualTo(List.of(
                Map.of("wins", 3, "reached", 0, "canReach", 3), // the loser can no longer reach 3
                Map.of("wins", 2, "reached", 0, "canReach", 4)));
        // The same tap again (a retry from the offline queue) changes nothing.
        assertThat(setWinner(meleeId, matchId, winner)).hasStatusOk();
        assertThat(json(melees.get(club, meleeId), "$.teams[*].wins").toString())
                .isEqualTo(json(result, "$.teams[*].wins").toString());
    }

    @Test
    void aResultCanBeCorrectedOrCleared() {
        long meleeId = meleeWithPlayers(6); // 3 teams: 3 rounds possible
        draw(meleeId, false, false);
        MvcTestResult schedule = generate(meleeId, false);
        long matchId = id(schedule, "$.rounds[0].matches[0].id");
        long teamA = id(schedule, "$.rounds[0].matches[0].teamAId");
        long teamB = id(schedule, "$.rounds[0].matches[0].teamBId");
        setWinner(meleeId, matchId, teamA);

        assertThat(setWinner(meleeId, matchId, teamB)).bodyJson()
                .extractingPath("$.rounds[0].matches[0].winnerTeamId").isEqualTo((int) teamB);
        assertThat(melees.send(club, "PUT", "/api/melees/%d/schedule/matchups/%d/winner".formatted(meleeId, matchId),
                "{\"winnerTeamId\":null}")).bodyJson().extractingPath("$.rounds[0].matches[0].winnerTeamId").isNull();
    }

    @Test
    void aTeamThatDoesNotPlayTheMatchCannotWinIt() {
        long meleeId = meleeWithPlayers(8);
        draw(meleeId, false, false);
        MvcTestResult schedule = generate(meleeId, false);
        long matchId = id(schedule, "$.rounds[0].matches[0].id");
        long outsider = id(schedule, "$.rounds[0].matches[1].teamAId");

        assertThat(setWinner(meleeId, matchId, outsider))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.winnerTeamId").isEqualTo("NotInMatch");
    }

    @Test
    void goingBackKeepsTheWorkAndRedoingItAsksFirst() {
        long meleeId = meleeWithPlayers(8);
        draw(meleeId, false, false);
        MvcTestResult schedule = generate(meleeId, false);
        long matchId = id(schedule, "$.rounds[0].matches[0].id");
        setWinner(meleeId, matchId, id(schedule, "$.rounds[0].matches[0].teamAId"));

        // Back to the teams: the schedule and its result are still there.
        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null);
        assertThat(generate(meleeId, false))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.losses.results").isEqualTo(1);
        MvcTestResult resumed = melees.send(club, "POST", "/api/melees/%d/schedule/resume".formatted(meleeId), null);
        assertThat(resumed).bodyJson().extractingPath("$.status").isEqualTo("MATCHES");
        assertThat(resumed).bodyJson().extractingPath("$.rounds[0].matches[0].winnerTeamId").isNotNull();

        // Back twice, to the sign-up list: redrawing would lose the result, so it must be confirmed.
        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null);
        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null);
        assertThat(draw(meleeId, false, false)).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("CONFIRMATION_REQUIRED");
        MvcTestResult redrawn = draw(meleeId, false, true);
        assertThat(redrawn).hasStatusOk().bodyJson().extractingPath("$.rounds").asArray().isEmpty();
    }

    @Test
    void moreRoundsThanTheTeamsAllowAreRejected() {
        long meleeId = meleeWithPlayers(4); // 2 teams: only one round is possible
        draw(meleeId, false, false);

        assertThat(generate(meleeId, false))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.maxRounds").isEqualTo(1);
    }

    @Test
    void moreMatchesThanCourtsAreOnlyPlayedOffCourtOnceAccepted() {
        long meleeId = meleeWithPlayers(40); // 20 teams, 10 matches per round, 8 courts: 2 off court
        draw(meleeId, false, false);

        MvcTestResult refused = generate(meleeId, false);
        assertThat(refused).hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.code").isEqualTo("OFF_COURT_MATCHES");
        assertThat(refused).bodyJson().extractingPath("$.offCourtMatches").isEqualTo(2);
        assertThat(refused).bodyJson().extractingPath("$.courts").isEqualTo(8);

        MvcTestResult schedule = generate(meleeId, false, true);
        assertThat(schedule).hasStatusOk();
        for (int round = 0; round < 3; round++) {
            List<Integer> courts = json(schedule, "$.rounds[%d].matches[*].courtNumber".formatted(round));
            assertThat(courts).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8, null, null);
        }
    }

    @Test
    void fewerMatchesThanCourtsNeedNoQuestionAndTakeTheFirstCourts() {
        long meleeId = meleeWithPlayers(12); // 6 teams, 3 matches per round, 8 courts

        draw(meleeId, false, false);
        MvcTestResult schedule = generate(meleeId, false);

        assertThat(schedule).hasStatusOk();
        List<Integer> courts = json(schedule, "$.rounds[*].matches[*].courtNumber");
        assertThat(courts).containsOnly(1, 2, 3);
    }

    @Test
    void anOffCourtMatchGetsACourtOnceOneIsFree() {
        long meleeId = meleeWithPlayers(40); // 20 teams, 10 matches per round, 8 courts: 2 off court
        draw(meleeId, false, false);
        MvcTestResult schedule = generate(meleeId, false, true);
        List<Map<String, Object>> firstRound = json(schedule, "$.rounds[0].matches");
        Map<String, Object> waiting = firstRound.stream().filter(match -> match.get("courtNumber") == null).findFirst().orElseThrow();
        Map<String, Object> onCourtOne = firstRound.stream().filter(match -> Integer.valueOf(1).equals(match.get("courtNumber")))
                .findFirst().orElseThrow();
        long waitingId = ((Number) waiting.get("id")).longValue();

        assertThat(assignCourt(meleeId, waitingId, 1))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("COURT_OCCUPIED");

        setWinner(meleeId, ((Number) onCourtOne.get("id")).longValue(), ((Number) onCourtOne.get("teamAId")).longValue());
        MvcTestResult assigned = assignCourt(meleeId, waitingId, 1);
        List<Map<String, Object>> after = json(assigned, "$.rounds[0].matches");
        assertThat(after).filteredOn(match -> ((Number) match.get("id")).longValue() == waitingId)
                .extracting(match -> match.get("courtNumber")).containsExactly(1);
    }

    @Test
    void someoneWhoLeavesDuringTheMatchesCanBeReplaced() {
        long meleeId = meleeWithPlayers(6); // 3 teams: 3 rounds possible
        MvcTestResult drawn = draw(meleeId, false, false);
        long teamId = id(drawn, "$.teams[0].id");
        long leaving = id(drawn, "$.teams[0].memberIds[0]");
        assertThat(generate(meleeId, false)).bodyJson().extractingPath("$.status").isEqualTo("MATCHES");
        melees.send(club, "POST", "/api/melees/%d/participants/%d/withdraw".formatted(meleeId, leaving), null);
        long joining = id(melees.send(club, "POST", "/api/melees/%d/participants".formatted(meleeId),
                "{\"name\":\"Suplente\"}"), "$.participants[6].id");

        MvcTestResult result = melees.send(club, "POST", "/api/melees/%d/teams/substitute".formatted(meleeId),
                "{\"leavingPlayerId\":%d,\"joiningPlayerId\":%d}".formatted(leaving, joining));

        assertThat(result).hasStatusOk().bodyJson().extractingPath("$.teams[0].id").isEqualTo((int) teamId);
        List<Integer> members = json(result, "$.teams[0].memberIds");
        assertThat(members).contains((int) joining).doesNotContain((int) leaving);
        assertThat(result).bodyJson().extractingPath("$.teamIssues.membersNotPlaying").asArray().isEmpty();
    }

    @Test
    void anotherClubCannotDrawOrRecordResults() {
        long meleeId = meleeWithPlayers(6); // 3 teams: 3 rounds possible
        draw(meleeId, false, false);
        MvcTestResult schedule = generate(meleeId, false);
        long matchId = id(schedule, "$.rounds[0].matches[0].id");
        RegisteredClub other = clubs.register("Otro club");

        assertThat(melees.send(other, "POST", "/api/melees/%d/teams/draw".formatted(meleeId), "{}"))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(melees.send(other, "PUT", "/api/melees/%d/schedule/matchups/%d/winner".formatted(meleeId, matchId),
                "{\"winnerTeamId\":1}")).hasStatus(HttpStatus.NOT_FOUND);
    }

    private long meleeWithPlayers(int players) {
        long meleeId = melees.create(club, 2);
        melees.signUp(club, meleeId, players);
        return meleeId;
    }

    private MvcTestResult draw(long meleeId, boolean acceptDifferentTeam, boolean confirmLosses) {
        return melees.send(club, "POST", "/api/melees/%d/teams/draw".formatted(meleeId),
                "{\"acceptDifferentTeam\":%s,\"confirmLosses\":%s}".formatted(acceptDifferentTeam, confirmLosses));
    }

    private MvcTestResult generate(long meleeId, boolean confirmLosses) {
        return generate(meleeId, confirmLosses, false);
    }

    private MvcTestResult generate(long meleeId, boolean confirmLosses, boolean acceptOffCourt) {
        return melees.send(club, "POST", "/api/melees/%d/schedule/generate".formatted(meleeId),
                "{\"confirmLosses\":%s,\"acceptOffCourt\":%s}".formatted(confirmLosses, acceptOffCourt));
    }

    private MvcTestResult setWinner(long meleeId, long matchId, long winnerTeamId) {
        return melees.send(club, "PUT", "/api/melees/%d/schedule/matchups/%d/winner".formatted(meleeId, matchId),
                "{\"winnerTeamId\":%d}".formatted(winnerTeamId));
    }

    private MvcTestResult assignCourt(long meleeId, long matchId, int court) {
        return melees.send(club, "PUT", "/api/melees/%d/schedule/matchups/%d/court".formatted(meleeId, matchId),
                "{\"courtNumber\":%d}".formatted(court));
    }

    private static long id(MvcTestResult result, String path) {
        return ((Number) json(result, path)).longValue();
    }
}
