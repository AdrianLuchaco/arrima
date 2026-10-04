package es.arrima;

import es.arrima.push.VapidKeys;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Real PostgreSQL for integration tests, same major version as Supabase. Spring caches the test
 * context, so every test class that uses @IntegrationTest shares one container.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));
    }

    /** A fresh random signing secret per test run: no secret-looking literal in the repository. */
    @Bean
    DynamicPropertyRegistrar testJwtSecret() {
        String secret = TestSecrets.randomJwtSecret();
        return registry -> registry.add("arrima.security.jwt-secret", () -> secret);
    }

    /** Fresh VAPID keys too, so Web Push is on in the tests (the gateway below sends nothing). */
    @Bean
    DynamicPropertyRegistrar testVapidKeys() {
        VapidKeys keys = VapidKeys.generate();
        return registry -> {
            registry.add("arrima.push.vapid-public-key", keys::publicKeyBase64Url);
            registry.add("arrima.push.vapid-private-key", keys::privateKeyBase64Url);
            registry.add("arrima.push.subject", () -> "mailto:pruebas@arrima.test");
        };
    }

    /** Notifications are kept for the tests to read instead of being sent to Google or Apple. */
    @Bean
    @Primary
    RecordingPushGateway recordingPushGateway() {
        return new RecordingPushGateway();
    }

    /** E-mails are kept for the tests to read instead of being sent. */
    @Bean
    @Primary
    RecordingEmailSender recordingEmailSender() {
        return new RecordingEmailSender();
    }

    /** Replaces the system clock everywhere; tests that move it must call reset() afterwards. */
    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }
}
