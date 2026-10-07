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
    void outsideRenderTheVersionIsLocal() {
        assertThat(mvc.get().uri("/api/health")).bodyJson().extractingPath("$.version").isEqualTo("local");
        assertThat(mvc.get().uri("/api/health/db")).bodyJson().extractingPath("$.version").isEqualTo("local");
    }

    @Test
    void onRenderTheVersionIsTheShortCommit() {
        HealthController onRender = new HealthController(null, "c5b11d1f0e2a9b8c7d6e5f4a3b2c1d0e9f8a7b6c");

        assertThat(onRender.liveness()).isEqualTo(new HealthResponse("UP", "c5b11d1"));
    }

    @Test
    void databaseCheckRunsARealQuery() {
        assertThat(mvc.get().uri("/api/health/db"))
                .hasStatusOk()
                .bodyJson().extractingPath("$.status").isEqualTo("UP");
    }

    @Test
    void anyOtherEndpointIsDeniedByDefault() {
        assertThat(mvc.get().uri("/api/unknown")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post().uri("/api/health")).hasStatus(HttpStatus.UNAUTHORIZED);
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
