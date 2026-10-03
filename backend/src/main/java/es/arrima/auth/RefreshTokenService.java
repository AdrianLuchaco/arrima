package es.arrima.auth;

import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import es.arrima.shared.security.SecureTokens;
import es.arrima.shared.security.SecurityProperties;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Rotating refresh tokens:
 * <ul>
 *   <li>every use returns a new token and marks the old one as rotated;</li>
 *   <li>presenting a rotated token again means someone kept a copy: the whole family is revoked
 *       and both the thief and the real admin must log in again;</li>
 *   <li>except during a short grace period after the rotation. On the courts the connection is bad:
 *       the server may rotate the token while the response never reaches the phone, which then
 *       retries with the old token. That must not log the admin out in the middle of a melee.</li>
 * </ul>
 * Callers run inside a transaction (AuthService).
 */
@Service
class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    private final RefreshTokenRepository repository;
    private final SecurityProperties properties;

    RefreshTokenService(RefreshTokenRepository repository, SecurityProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    /** Starts a new family, on login or registration. */
    IssuedRefreshToken startFamily(long adminId, Instant now) {
        repository.deleteExpiredOfAdmin(adminId, now);
        return issue(adminId, UUID.randomUUID(), now);
    }

    /** Validates the presented token and returns its replacement, or rejects it. */
    RotatedRefreshToken rotate(String rawToken, Instant now) {
        RefreshToken current = repository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .filter(token -> token.isUsable(now))
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (current.isRotated()) {
            if (!current.isWithinReuseGrace(now, properties.refreshReuseGrace())) {
                log.warn("Refresh token reused after rotation: revoking family {} of admin {}",
                        current.getFamilyId(), current.getAdminId());
                repository.revokeFamily(current.getFamilyId(), now);
                throw new ApiException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
        } else {
            current.markRotated(now);
        }
        return new RotatedRefreshToken(current.getAdminId(), issue(current.getAdminId(), current.getFamilyId(), now));
    }

    /** Logout: the token and every other token from the same login stop working. */
    void revokeFamilyOf(String rawToken, Instant now) {
        repository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
                .ifPresent(token -> repository.revokeFamily(token.getFamilyId(), now));
    }

    /** After a password change, every session of the admin ends. */
    void revokeAllOfAdmin(long adminId, Instant now) {
        repository.revokeAllOfAdmin(adminId, now);
    }

    private IssuedRefreshToken issue(long adminId, UUID familyId, Instant now) {
        String rawToken = SecureTokens.generate();
        RefreshToken token = repository.save(new RefreshToken(adminId, familyId, SecureTokens.sha256Hex(rawToken),
                now, properties.refreshTokenTtl()));
        return new IssuedRefreshToken(rawToken, token.getExpiresAt());
    }

    record IssuedRefreshToken(String value, Instant expiresAt) {
    }

    record RotatedRefreshToken(long adminId, IssuedRefreshToken replacement) {
    }
}
