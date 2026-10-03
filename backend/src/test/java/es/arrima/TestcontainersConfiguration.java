package es.arrima;

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

    /** Replaces the system clock everywhere; tests that move it must call reset() afterwards. */
    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }
}
