package es.arrima.auth;

import es.arrima.shared.security.SecurityProperties;
import java.time.Clock;
import java.time.Duration;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The refresh token cookie:
 * <ul>
 *   <li>HttpOnly: JavaScript (and so any injected script) cannot read it;</li>
 *   <li>Secure: only sent over HTTPS;</li>
 *   <li>SameSite=Strict: never sent on requests started by another site;</li>
 *   <li>Path=/api/auth: only sent to the endpoints that need it, not on every API call.</li>
 * </ul>
 * Thanks to Vercel's rewrite the API is on the same domain as the app, so Safari treats it as a
 * first-party cookie and does not block it.
 */
@Component
class RefreshTokenCookie {

    static final String NAME = "arrima_refresh";
    private static final String PATH = "/api/auth";

    private final SecurityProperties properties;
    private final Clock clock;

    RefreshTokenCookie(SecurityProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    String create(IssuedTokens tokens) {
        Duration maxAge = Duration.between(clock.instant(), tokens.refreshTokenExpiresAt());
        return base(tokens.refreshToken()).maxAge(maxAge).build().toString();
    }

    String clear() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(properties.cookieSecure())
                .sameSite("Strict")
                .path(PATH);
    }
}
