package es.arrima.auth;

import java.time.Instant;

/**
 * What a successful login, registration or refresh produces: the access token goes in the response
 * body (the frontend keeps it in memory) and the refresh token in an HttpOnly cookie.
 */
record IssuedTokens(String accessToken, Instant accessTokenExpiresAt, String refreshToken, Instant refreshTokenExpiresAt) {
}
