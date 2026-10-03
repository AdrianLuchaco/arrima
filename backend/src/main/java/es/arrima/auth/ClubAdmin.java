package es.arrima.auth;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** The account that manages a club (one per club for now). */
@Entity
@Table(name = "club_admin")
public class ClubAdmin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clubId;

    private String email;

    private String passwordHash;

    private Instant passwordChangedAt;

    private Instant lastLoginAt;

    private Instant createdAt;

    protected ClubAdmin() {
        // for JPA
    }

    public ClubAdmin(long clubId, String email, String passwordHash, Instant now) {
        this.clubId = clubId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordChangedAt = now;
        this.createdAt = now;
    }

    public void recordLogin(Instant now) {
        this.lastLoginAt = now;
    }

    public void changePasswordHash(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.passwordChangedAt = now;
    }

    public Long getId() {
        return id;
    }

    public long getClubId() {
        return clubId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
