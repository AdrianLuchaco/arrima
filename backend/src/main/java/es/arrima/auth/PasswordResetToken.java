package es.arrima.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** The token of a "choose a new password" link sent by e-mail. Only its hash is stored. */
@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long adminId;

    private String tokenHash;

    private Instant createdAt;

    private Instant expiresAt;

    private Instant usedAt;

    protected PasswordResetToken() {
        // for JPA
    }

    PasswordResetToken(long adminId, String tokenHash, Instant now, Instant expiresAt) {
        this.adminId = adminId;
        this.tokenHash = tokenHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }

    long getAdminId() {
        return adminId;
    }
}
