package es.arrima;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import es.arrima.auth.SignupInvitation;
import es.arrima.auth.SignupInvitationRepository;
import es.arrima.shared.security.SecureTokens;
import jakarta.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Creates clubs through the real registration endpoint, as a club admin would. */
public class TestClubs {

    public static final String PASSWORD = "contraseña-de-prueba";

    private final MockMvcTester mvc;
    private final SignupInvitationRepository invitations;

    public TestClubs(MockMvcTester mvc, SignupInvitationRepository invitations) {
        this.mvc = mvc;
        this.invitations = invitations;
    }

    public String newInvitationCode() {
        String code = UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        Instant now = Instant.now();
        invitations.save(new SignupInvitation(SecureTokens.sha256Hex(SignupInvitation.normalizeCode(code)),
                "test", now, now.plus(Duration.ofDays(30))));
        return code;
    }

    public RegisteredClub register(String clubName) {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        MvcTestResult result = mvc.post().uri("/api/auth/register")
                .header("X-Forwarded-For", randomIp())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"invitationCode":"%s","clubName":"%s","email":"%s","password":"%s"}"""
                        .formatted(newInvitationCode(), clubName, email, PASSWORD))
                .exchange();
        assertThat(result).hasStatus(HttpStatus.CREATED);
        String accessToken = json(result, "$.accessToken");
        Cookie refreshCookie = result.getResponse().getCookie("arrima_refresh");
        return new RegisteredClub(email, accessToken, refreshCookie);
    }

    /** Reads a value from a JSON response body. */
    public static <T> T json(MvcTestResult result, String path) {
        try {
            return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), path);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Each registration from a different address, so the per-IP limit never interferes with tests. */
    public static String randomIp() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        return "10.%d.%d.%d".formatted(random.nextInt(256), random.nextInt(256), random.nextInt(1, 255));
    }

    public record RegisteredClub(String email, String accessToken, Cookie refreshCookie) {

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }
}
