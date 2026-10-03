package es.arrima;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.TestClubs.RegisteredClub;
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

    public long create(RegisteredClub club, int teamSize) {
        MvcTestResult result = send(club, "POST", "/api/melees", "{\"teamSize\":%d}".formatted(teamSize));
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
