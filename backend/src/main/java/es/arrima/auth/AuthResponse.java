package es.arrima.auth;

import java.time.Instant;

/** The refresh token is not here: it travels only in the HttpOnly cookie, out of JavaScript's reach. */
record AuthResponse(String accessToken, Instant expiresAt) {

    static AuthResponse from(IssuedTokens tokens) {
        return new AuthResponse(tokens.accessToken(), tokens.accessTokenExpiresAt());
    }
}
