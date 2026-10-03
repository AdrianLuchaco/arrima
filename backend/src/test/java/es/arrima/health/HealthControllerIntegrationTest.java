package es.arrima.health;

import static org.assertj.core.api.Assertions.assertThat;

import es.arrima.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class HealthControllerIntegrationTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void livenessAnswersWithoutAuthentication() {
        assertThat(mvc.get().uri("/api/health"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void databaseCheckRunsARealQuery() {
        assertThat(mvc.get().uri("/api/health/db"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void anyOtherEndpointIsDeniedByDefault() {
        assertThat(mvc.get().uri("/api/clubs")).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.post().uri("/api/health")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void responsesCarrySecurityHeaders() {
        assertThat(mvc.get().uri("/api/health"))
                .hasHeader("X-Content-Type-Options", "nosniff")
                .hasHeader("X-Frame-Options", "DENY")
                .hasHeader("Referrer-Policy", "no-referrer")
                .hasHeader("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'");
    }
}
