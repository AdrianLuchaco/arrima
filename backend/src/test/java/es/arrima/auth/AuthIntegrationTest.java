package es.arrima.auth;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.MutableClock;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class AuthIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    @Autowired
    private MutableClock clock;

    private TestClubs clubs;

    @BeforeEach
    void setUp() {
        clubs = new TestClubs(mvc, invitations);
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void registrationCreatesTheClubAndLogsTheAdminIn() {
        RegisteredClub club = clubs.register("Club Petanca Arrima");

        assertThat(club.refreshCookie()).isNotNull();
        assertThat(club.refreshCookie().isHttpOnly()).isTrue();
        assertThat(club.refreshCookie().getSecure()).isTrue();
        assertThat(club.refreshCookie().getPath()).isEqualTo("/api/auth");
        assertThat(club.refreshCookie().getAttribute("SameSite")).isEqualTo("Strict");
        assertThat(mvc.get().uri("/api/club").header(HttpHeaders.AUTHORIZATION, club.bearer()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.name").isEqualTo("Club Petanca Arrima");
    }

    @Test
    void registrationNeedsAValidInvitation() {
        assertThat(register("NOEXISTE1234", "nuevo@example.com"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("INVITATION_INVALID");
    }

    @Test
    void anInvitationCanOnlyBeUsedOnce() {
        String code = clubs.newInvitationCode();
        assertThat(register(code, "primero@example.com")).hasStatus(HttpStatus.CREATED);

        assertThat(register(code, "segundo@example.com"))
                .bodyJson().extractingPath("$.code").isEqualTo("INVITATION_INVALID");
    }

    @Test
    void invitationCodesIgnoreCaseSpacesAndDashes() {
        String code = clubs.newInvitationCode();
        String typedByAPerson = " " + code.substring(0, 6).toLowerCase() + "-" + code.substring(6) + " ";

        assertThat(register(typedByAPerson, "tecleado@example.com")).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void anEmailCanOnlyHaveOneAccount() {
        RegisteredClub existing = clubs.register("Club A");

        assertThat(register(clubs.newInvitationCode(), existing.email().toUpperCase()))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("EMAIL_TAKEN");
    }

    @Test
    void shortPasswordsAreRejected() {
        MvcTestResult result = mvc.post().uri("/api/auth/register")
                .header("X-Forwarded-For", TestClubs.randomIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"invitationCode":"%s","clubName":"Club","email":"corta@example.com","password":"bolas"}"""
                        .formatted(clubs.newInvitationCode()))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.password").isEqualTo("TooShort");
    }

    @Test
    void loginWorksWithTheRightPasswordAndEmailInAnyCase() {
        RegisteredClub club = clubs.register("Club B");

        assertThat(login(club.email().toUpperCase(), TestClubs.PASSWORD, TestClubs.randomIp()))
                .hasStatusOk()
                .bodyJson().extractingPath("$.accessToken").isNotNull();
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() {
        RegisteredClub club = clubs.register("Club C");

        assertThat(login(club.email(), "contraseña equivocada", TestClubs.randomIp()))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_CREDENTIALS");
        assertThat(login("nadie@example.com", "contraseña equivocada", TestClubs.randomIp()))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_CREDENTIALS");
    }

    @Test
    void repeatedLoginAttemptsOnOneAccountAreLimited() {
        RegisteredClub club = clubs.register("Club D");
        for (int attempt = 0; attempt < 10; attempt++) {
            login(club.email(), "contraseña equivocada", TestClubs.randomIp());
        }

        // Even the right password is refused now, and from any address: the limit is per account.
        MvcTestResult result = login(club.email(), TestClubs.PASSWORD, TestClubs.randomIp());

        assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(result).headers().containsHeader(HttpHeaders.RETRY_AFTER);
    }

    @Test
    void refreshRotatesTheToken() {
        RegisteredClub club = clubs.register("Club E");

        MvcTestResult refreshed = refresh(club.refreshCookie());

        assertThat(refreshed).hasStatusOk().bodyJson().extractingPath("$.accessToken").isNotNull();
        Cookie newCookie = refreshed.getResponse().getCookie("arrima_refresh");
        assertThat(newCookie.getValue()).isNotEqualTo(club.refreshCookie().getValue());
        assertThat(refresh(newCookie)).hasStatusOk();
    }

    @Test
    void retryingWithTheOldTokenRightAfterRotationIsAccepted() {
        RegisteredClub club = clubs.register("Club F");
        refresh(club.refreshCookie());

        // The response with the new token was lost on a bad connection: the phone retries.
        clock.advance(Duration.ofSeconds(20));

        assertThat(refresh(club.refreshCookie())).hasStatusOk();
    }

    @Test
    void reusingARotatedTokenLaterRevokesTheWholeFamily() {
        RegisteredClub club = clubs.register("Club G");
        Cookie current = refresh(club.refreshCookie()).getResponse().getCookie("arrima_refresh");

        clock.advance(Duration.ofMinutes(5));

        assertThat(refresh(club.refreshCookie()))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("INVALID_REFRESH_TOKEN");
        // The legitimate holder's current token is revoked too: whoever has it must log in again.
        assertThat(refresh(current)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logoutRevokesTheRefreshTokenAndClearsTheCookie() {
        RegisteredClub club = clubs.register("Club H");

        MvcTestResult logout = mvc.post().uri("/api/auth/logout").cookie(club.refreshCookie()).exchange();

        assertThat(logout).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(logout.getResponse().getCookie("arrima_refresh").getMaxAge()).isZero();
        assertThat(refresh(club.refreshCookie())).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cookieEndpointsRejectCrossSiteRequests() {
        RegisteredClub club = clubs.register("Club I");

        assertThat(mvc.post().uri("/api/auth/refresh").cookie(club.refreshCookie()).header("Sec-Fetch-Site", "cross-site"))
                .hasStatus(HttpStatus.FORBIDDEN)
                .bodyJson().extractingPath("$.code").isEqualTo("CROSS_SITE_REQUEST");
        assertThat(mvc.post().uri("/api/auth/refresh").cookie(club.refreshCookie()).header("Sec-Fetch-Site", "same-origin"))
                .hasStatusOk();
    }

    @Test
    void accessTokensExpire() {
        RegisteredClub club = clubs.register("Club J");

        clock.advance(Duration.ofMinutes(16));

        assertThat(mvc.get().uri("/api/club").header(HttpHeaders.AUTHORIZATION, club.bearer()))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHENTICATED");
    }

    @Test
    void aTamperedAccessTokenIsRejected() {
        RegisteredClub club = clubs.register("Club K");
        String[] parts = club.accessToken().split("\\.");
        // Same signature, but the payload now claims another club.
        String forged = parts[0] + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"iss\":\"arrima\",\"sub\":\"1\",\"club\":1,\"exp\":9999999999}".getBytes())
                + "." + parts[2];

        assertThat(mvc.get().uri("/api/club").header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void aHugeBodyIsRejectedBeforeBeingRead() {
        String huge = "{\"email\":\"" + "a".repeat(600 * 1024) + "\",\"password\":\"x\"}";

        assertThat(mvc.post().uri("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(huge))
                .hasStatus(HttpStatus.CONTENT_TOO_LARGE)
                .bodyJson().extractingPath("$.code").isEqualTo("REQUEST_TOO_LARGE");
    }

    private MvcTestResult register(String invitationCode, String email) {
        return mvc.post().uri("/api/auth/register")
                .header("X-Forwarded-For", TestClubs.randomIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"invitationCode":"%s","clubName":"Club","email":"%s","password":"%s"}"""
                        .formatted(invitationCode, email, TestClubs.PASSWORD))
                .exchange();
    }

    private MvcTestResult login(String email, String password, String ip) {
        return mvc.post().uri("/api/auth/login")
                .header("X-Forwarded-For", ip)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))
                .exchange();
    }

    private MvcTestResult refresh(Cookie cookie) {
        return mvc.post().uri("/api/auth/refresh").cookie(cookie).exchange();
    }
}
