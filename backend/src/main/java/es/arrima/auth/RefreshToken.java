package es.arrima.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * One refresh token. Only its hash is stored. Each use "rotates" it: it is marked as rotated and a
 * new token of the same family replaces it. All tokens descending from one login share a family.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;

    private UUID familyId;

    private String tokenHash;

    private Instant issuedAt;

    private Instant expiresAt;

    private Instant rotatedAt;

    private Instant revokedAt;

    protected RefreshToken() {
        // for JPA
    }

    RefreshToken(long adminId, UUID familyId, String tokenHash, Instant now, Duration lifetime) {
        this.adminId = adminId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.issuedAt = now;
        this.expiresAt = now.plus(lifetime);
    }

    boolean isUsable(Instant now) {
        return revokedAt == null && now.isBefore(expiresAt);
    }

    boolean isRotated() {
        return rotatedAt != null;
    }

    /** True while an already rotated token may still be presented without being taken as stolen. */
    boolean isWithinReuseGrace(Instant now, Duration grace) {
        return rotatedAt != null && now.isBefore(rotatedAt.plus(grace));
    }

    void markRotated(Instant now) {
        this.rotatedAt = now;
    }

    long getAdminId() {
        return adminId;
    }

    UUID getFamilyId() {
        return familyId;
    }

    Instant getExpiresAt() {
        return expiresAt;
    }
}
