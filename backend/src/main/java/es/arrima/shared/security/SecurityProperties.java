package es.arrima.shared.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param jwtSecret          base64 of at least 32 random bytes (JWT_SECRET); also the root of the file-link key
 * @param accessTokenTtl     lifetime of the JWT access token
 * @param refreshTokenTtl    lifetime of each refresh token (renewed on every rotation)
 * @param refreshReuseGrace  how long an already rotated refresh token is still accepted, so a lost
 *                           response on a bad connection does not look like token theft
 * @param cookieSecure       Secure flag of the refresh cookie (only false for plain-HTTP local tests)
 */
@ConfigurationProperties("arrima.security")
public record SecurityProperties(
        String jwtSecret,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration refreshReuseGrace,
        boolean cookieSecure) {
}
