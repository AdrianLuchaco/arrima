package es.arrima.auth;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import es.arrima.MutableClock;
import es.arrima.RecordingEmailSender;
import es.arrima.TestClubs;
import es.arrima.TestClubs.RegisteredClub;
import es.arrima.mail.Email;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class PasswordResetIntegrationTest {

    private static final Pattern LINK = Pattern.compile("https://arrima\\.test/restablecer#([A-Za-z0-9_-]{43})");
    private static final String NEW_PASSWORD = "una contraseña nueva";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SignupInvitationRepository invitations;

    @Autowired
    private RecordingEmailSender emails;

    @Autowired
    private MutableClock clock;

    private RegisteredClub club;

    @BeforeEach
    void setUp() {
        club = new TestClubs(mvc, invitations).register("Club Recuperación");
        emails.clear();
    }

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    @Test
    void theLinkInTheEmailSetsANewPasswordAndEndsEverySession() {
        assertThat(requestReset(club.email())).hasStatus(HttpStatus.ACCEPTED);

        Email email = emails.awaitNextTo(club.email());
        assertThat(email.text()).contains("Club Recuperación");

        assertThat(confirm(tokenIn(email), NEW_PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(login(club.email(), NEW_PASSWORD)).hasStatusOk();
        assertThat(login(club.email(), TestClubs.PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
        // A session open elsewhere (maybe someone else's) is over.
        assertThat(mvc.post().uri("/api/auth/refresh").cookie(club.refreshCookie()))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void anUnknownEmailGetsTheSameAnswerAndNoEmailIsSent() {
        String unknown = "nadie-" + club.email();
        assertThat(requestReset(unknown)).hasStatus(HttpStatus.ACCEPTED);

        assertThat(emails.sendsNothingTo(unknown, Duration.ofMillis(500))).isTrue();
    }

    @Test
    void theEmailIsMatchedWithoutCaringAboutCapitalsOrSpaces() {
        requestReset("  " + club.email().toUpperCase() + " ");

        assertThat(emails.awaitNextTo(club.email()).subject()).isEqualTo("Cambia la contraseña de Arrima");
    }

    @Test
    void aLinkWorksOnlyOnce() {
        requestReset(club.email());
        String token = tokenIn(emails.awaitNextTo(club.email()));
        confirm(token, NEW_PASSWORD);

        assertThat(confirm(token, "otra contraseña distinta"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("RESET_LINK_INVALID");
    }

    @Test
    void aLinkExpiresAfterThirtyMinutes() {
        requestReset(club.email());
        String token = tokenIn(emails.awaitNextTo(club.email()));

        clock.advance(Duration.ofMinutes(31));

        assertThat(confirm(token, NEW_PASSWORD))
                .bodyJson().extractingPath("$.code").isEqualTo("RESET_LINK_INVALID");
    }

    @Test
    void askingAgainCancelsThePreviousLink() {
        requestReset(club.email());
        String first = tokenIn(emails.awaitNextTo(club.email()));
        requestReset(club.email());
        String second = tokenIn(emails.awaitNextTo(club.email()));

        assertThat(confirm(first, NEW_PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(confirm(second, NEW_PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void aTooShortPasswordCanBeCorrectedWithTheSameLink() {
        requestReset(club.email());
        String token = tokenIn(emails.awaitNextTo(club.email()));

        assertThat(confirm(token, "corta"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.fields.password").isEqualTo("TooShort");
        assertThat(confirm(token, NEW_PASSWORD)).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void anInventedTokenIsRejected() {
        assertThat(confirm("x".repeat(43), NEW_PASSWORD))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.code").isEqualTo("RESET_LINK_INVALID");
    }

    @Test
    void onlyThreeEmailsAnHourCanBeSentToOneAddress() {
        for (int i = 0; i < 3; i++) {
            assertThat(requestReset(club.email())).hasStatus(HttpStatus.ACCEPTED);
        }

        assertThat(requestReset(club.email()))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .bodyJson().extractingPath("$.code").isEqualTo("RATE_LIMITED");
    }

    private MvcTestResult requestReset(String email) {
        return mvc.post().uri("/api/auth/password-reset/request")
                .header("X-Forwarded-For", TestClubs.randomIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s"}""".formatted(email))
                .exchange();
    }

    private MvcTestResult confirm(String token, String password) {
        return mvc.post().uri("/api/auth/password-reset/confirm")
                .header("X-Forwarded-For", TestClubs.randomIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s","password":"%s"}""".formatted(token, password))
                .exchange();
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post().uri("/api/auth/login")
                .header("X-Forwarded-For", TestClubs.randomIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}""".formatted(email, password))
                .exchange();
    }

    private static String tokenIn(Email email) {
        Matcher matcher = LINK.matcher(email.text());
        assertThat(matcher.find()).as("link in the e-mail").isTrue();
        return matcher.group(1);
    }
}
