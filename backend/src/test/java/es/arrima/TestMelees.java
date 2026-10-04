package es.arrima;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.TestClubs.RegisteredClub;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Melee steps through the real API, for integration tests. */
public class TestMelees {

    private final MockMvcTester mvc;

    public TestMelees(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    /** Without an entry fee, so tests about teams and matches don't need to record payments. */
    public long create(RegisteredClub club, int teamSize) {
        return create(club, teamSize, 0);
    }

    public long create(RegisteredClub club, int teamSize, int entryFeeCents) {
        MvcTestResult result = send(club, "POST", "/api/melees",
                "{\"teamSize\":%d,\"entryFeeCents\":%d}".formatted(teamSize, entryFeeCents));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ((Number) TestClubs.json(result, "$.id")).longValue();
    }

    /** Signs up "Jugador 1".."Jugador n" with their list numbers. */
    public MvcTestResult signUp(RegisteredClub club, long meleeId, int players) {
        String participants = IntStream.rangeClosed(1, players)
                .mapToObj(n -> "{\"listNumber\":%d,\"name\":\"Jugador %d\"}".formatted(n, n))
                .collect(Collectors.joining(","));
        MvcTestResult result = send(club, "POST", "/api/melees/%d/participants/import".formatted(meleeId),
                "{\"participants\":[" + participants + "]}");
        assertThat(result).hasStatusOk();
        return result;
    }

    /**
     * Four doublettes, three rounds (everyone meets everyone), results chosen so that teams 1 and 2
     * end with 2 wins and teams 3 and 4 with 1: both pairs must play la Internacional.
     */
    public ScenarioMelee fourTeamsWithTies(RegisteredClub club, int prizeCount) {
        MvcTestResult created = send(club, "POST", "/api/melees",
                "{\"teamSize\":2,\"prizeCount\":%d,\"entryFeeCents\":0}".formatted(prizeCount));
        long meleeId = ((Number) TestClubs.json(created, "$.id")).longValue();
        signUp(club, meleeId, 8);
        send(club, "POST", "/api/melees/%d/teams/draw".formatted(meleeId), "{}");
        MvcTestResult schedule = send(club, "POST", "/api/melees/%d/schedule/generate".formatted(meleeId), "{}");

        Map<Integer, Long> teamIdOf = new HashMap<>();
        Map<Long, Integer> numberOf = new HashMap<>();
        List<Map<String, Object>> teams = TestClubs.json(schedule, "$.teams");
        teams.forEach(team -> {
            long id = ((Number) team.get("id")).longValue();
            teamIdOf.put((Integer) team.get("number"), id);
            numberOf.put(id, (Integer) team.get("number"));
        });
        Map<String, Integer> winners = Map.of("1-2", 1, "1-3", 3, "1-4", 1, "2-3", 2, "2-4", 2, "3-4", 4);
        List<Map<String, Object>> matches = TestClubs.json(schedule, "$.rounds[*].matches[*]");
        for (Map<String, Object> match : matches) {
            int a = numberOf.get(((Number) match.get("teamAId")).longValue());
            int b = numberOf.get(((Number) match.get("teamBId")).longValue());
            int winner = winners.get(Math.min(a, b) + "-" + Math.max(a, b));
            send(club, "PUT", "/api/melees/%d/schedule/matchups/%s/winner".formatted(meleeId, match.get("id")),
                    "{\"winnerTeamId\":%d}".formatted(teamIdOf.get(winner)));
        }
        return new ScenarioMelee(meleeId, teamIdOf);
    }

    /** All six balls of a team: three pointing with one outcome, three shooting with another. */
    public MvcTestResult throwSix(RegisteredClub club, long meleeId, long roundId, long teamId, String pointing, String shooting) {
        MvcTestResult last = null;
        for (String kind : List.of("POINTING", "SHOOTING")) {
            for (int ball = 1; ball <= 3; ball++) {
                last = throwBall(club, meleeId, roundId, teamId, kind, ball, kind.equals("POINTING") ? pointing : shooting);
            }
        }
        return last;
    }

    public MvcTestResult throwBall(RegisteredClub club, long meleeId, long roundId, long teamId, String kind, int ball, String outcome) {
        return send(club, "PUT", "/api/melees/%d/international/rounds/%d/teams/%d/throws/%s/%d"
                .formatted(meleeId, roundId, teamId, kind, ball), "{\"outcome\":\"%s\"}".formatted(outcome));
    }

    public record ScenarioMelee(long meleeId, Map<Integer, Long> teamIdOf) {

        public long team(int number) {
            return teamIdOf.get(number);
        }
    }

    public MvcTestResult get(RegisteredClub club, long meleeId) {
        return send(club, "GET", "/api/melees/" + meleeId, null);
    }

    public MvcTestResult send(RegisteredClub club, String method, String uri, String json) {
        var request = switch (method) {
            case "GET" -> mvc.get().uri(uri);
            case "POST" -> mvc.post().uri(uri);
            case "PUT" -> mvc.put().uri(uri);
            case "DELETE" -> mvc.delete().uri(uri);
            default -> throw new IllegalArgumentException(method);
        };
        request.header(HttpHeaders.AUTHORIZATION, club.bearer());
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }
}
