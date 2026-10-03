package es.arrima.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;

/**
 * Single-use code required to register a club. The platform owner creates them with
 * scripts/create-invitation.sql and hands each one to a club; only the hash is stored.
 */
@Entity
@Table(name = "signup_invitation")
public class SignupInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String codeHash;

    private String note;

    private Instant createdAt;

    private Instant expiresAt;

    private Instant usedAt;

    private Long usedByClubId;

    protected SignupInvitation() {
        // for JPA
    }

    public SignupInvitation(String codeHash, String note, Instant now, Instant expiresAt) {
        this.codeHash = codeHash;
        this.note = note;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    boolean isUsable(Instant now) {
        return usedAt == null && now.isBefore(expiresAt);
    }

    void markUsed(long clubId, Instant now) {
        this.usedAt = now;
        this.usedByClubId = clubId;
    }

    /** Codes are typed by people: case, spaces and dashes are ignored ("ab12-cd34" equals "AB12CD34"). */
    public static String normalizeCode(String code) {
        return code == null ? "" : code.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }
}
