package es.arrima.api;

import static es.arrima.TestClubs.json;
import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.TestMelees;
import es.arrima.auth.SignupInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Phase 5: what spectators see with the code, without an account, and only for reading. */
@IntegrationTest
class PublicViewIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    private TestClubs clubs;
    private TestMelees melees;
    private RegisteredClub club;
    private long meleeId;
    private String code;

    @BeforeEach
    void setUp() {
        clubs = new TestClubs(mvc, invitations);
        melees = new TestMelees(mvc);
        club = clubs.register("Club Petanca");
        meleeId = melees.create(club, 2);
        code = json(melees.signUp(club, meleeId, 8), "$.publicCode");
    }

    @Test
    void anyoneWithTheCodeSeesTheMeleeWithoutAnAccount() {
        assertThat(mvc.get().uri("/api/public/melees/" + code))
                .hasStatusOk()
                .bodyJson().isLenientlyEqualTo("{\"publicCode\":\"%s\",\"status\":\"REGISTRATION\",\"club\":{\"name\":\"Club Petanca\"}}".formatted(code));
    }

    @Test
    void theCodeCanBeTypedInLowerCase() {
        assertThat(mvc.get().uri("/api/public/melees/" + code.toLowerCase())).hasStatusOk();
    }

    @Test
    void anUnchangedViewIsAnsweredWith304AndAChangeGivesANewEtag() {
        MvcTestResult first = mvc.get().uri("/api/public/melees/" + code).exchange();
        String etag = first.getResponse().getHeader(HttpHeaders.ETAG);
        assertThat(etag).isNotBlank();

        assertThat(mvc.get().uri("/api/public/melees/" + code).header(HttpHeaders.IF_NONE_MATCH, etag))
                .hasStatus(HttpStatus.NOT_MODIFIED);

        melees.send(club, "POST", "/api/melees/%d/participants".formatted(meleeId), "{\"name\":\"Nueva\"}");
        MvcTestResult changed = mvc.get().uri("/api/public/melees/" + code).header(HttpHeaders.IF_NONE_MATCH, etag).exchange();
        assertThat(changed).hasStatusOk();
        assertThat(changed.getResponse().getHeader(HttpHeaders.ETAG)).isNotEqualTo(etag);
    }

    @Test
    void nothingPublicCanWrite() {
        String base = "/api/public/melees/" + code;

        assertThat(mvc.post().uri(base)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.put().uri(base)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.delete().uri(base)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri(base + "/participants")).hasStatus(HttpStatus.UNAUTHORIZED);
        // And the admin endpoints of that melee still need the admin's token.
        assertThat(mvc.post().uri("/api/melees/%d/teams/draw".formatted(meleeId))).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void guessingCodesGetsBlocked() {
        String ip = TestClubs.randomIp();
        for (int attempt = 0; attempt < 30; attempt++) {
            assertThat(mvc.get().uri("/api/public/melees/ZZZZ%04d".formatted(attempt)).header("X-Forwarded-For", ip))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        // Blocked even for a real code now, from that address only.
        assertThat(mvc.get().uri("/api/public/melees/" + code).header("X-Forwarded-For", ip))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(mvc.get().uri("/api/public/melees/" + code).header("X-Forwarded-For", TestClubs.randomIp()))
                .hasStatusOk();
    }

    @Test
    void spectatorsOnlySeeThePhasesAlreadyReached() {
        melees.send(club, "POST", "/api/melees/%d/teams/draw".formatted(meleeId), "{}");
        melees.send(club, "POST", "/api/melees/%d/back".formatted(meleeId), null);

        // Back in the sign-up phase: the teams are kept for the admin, hidden from spectators.
        assertThat(melees.get(club, meleeId)).bodyJson().extractingPath("$.teams.length()").isEqualTo(4);
        assertThat(mvc.get().uri("/api/public/melees/" + code)).bodyJson().extractingPath("$.teams").asArray().isEmpty();
    }
}
