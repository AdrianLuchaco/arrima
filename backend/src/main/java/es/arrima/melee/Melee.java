package es.arrima.melee;

import es.arrima.club.MeleeSettings;
import es.arrima.club.ScoringTable;
import es.arrima.shared.error.ApiException;
import es.arrima.shared.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;
import org.hibernate.annotations.DynamicUpdate;

/**
 * A one-day tournament. It keeps its own copy of the club's settings and points.
 * <p>
 * {@code @DynamicUpdate}: only changed columns are written. "revision" and "last_activity_at" are
 * updated with an atomic SQL statement (MeleeRepository.recordChange), and a full-row update from
 * an entity loaded earlier would overwrite them with stale values.
 */
@Entity
@Table(name = "melee")
@DynamicUpdate
public class Melee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long clubId;

    private LocalDate playedOn;

    @Enumerated(EnumType.STRING)
    private MeleeFormat format;

    private int teamSize;

    @Enumerated(EnumType.STRING)
    private MeleeStatus status;

    private String publicCode;

    @Embedded
    private MeleeSettings settings;

    @Embedded
    private ScoringTable scoring;

    @Column(updatable = false)
    private Instant lastActivityAt;

    private Instant closedAt;

    private Instant createdAt;

    private Instant updatedAt;

    @Column(insertable = false, updatable = false)
    private long revision;

    protected Melee() {
        // for JPA
    }

    public Melee(long clubId, LocalDate playedOn, int teamSize, MeleeSettings settings, ScoringTable scoring,
            String publicCode, Instant now) {
        this.clubId = clubId;
        this.playedOn = playedOn;
        this.format = MeleeFormat.CLASSIC;
        this.teamSize = teamSize;
        this.status = MeleeStatus.REGISTRATION;
        this.settings = settings;
        this.scoring = scoring;
        this.publicCode = publicCode;
        this.lastActivityAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Rejects the operation unless the melee is in one of the given phases. */
    public void requireStatus(MeleeStatus... allowed) {
        if (Arrays.stream(allowed).noneMatch(candidate -> candidate == status)) {
            throw new ApiException(ErrorCode.INVALID_STATE, Map.of("status", status.name()));
        }
    }

    public void moveTo(MeleeStatus next, Instant now) {
        this.status = next;
        this.updatedAt = now;
    }

    /** One phase back. Nothing is deleted: moving forward again reconciles what already exists. */
    void goBack(Instant now) {
        requireStatus(MeleeStatus.TEAMS, MeleeStatus.MATCHES, MeleeStatus.INTERNATIONAL, MeleeStatus.PRIZES);
        moveTo(status.previous(), now);
    }

    void close(Instant now) {
        requireStatus(MeleeStatus.PRIZES);
        moveTo(MeleeStatus.CLOSED, now);
        this.closedAt = now;
    }

    /**
     * The entry fee decides who plays (with a fee, only those who paid), so it can only change
     * before the draw: afterwards it would silently change who should be in the teams.
     */
    public void changeSettings(MeleeSettings settings, Instant now) {
        if (settings.entryFeeCents() != this.settings.entryFeeCents()) {
            requireStatus(MeleeStatus.REGISTRATION);
        }
        this.settings = settings;
        this.updatedAt = now;
    }

    public boolean requiresPayment() {
        return settings.requiresPayment();
    }

    public Long getId() {
        return id;
    }

    public long getClubId() {
        return clubId;
    }

    public LocalDate getPlayedOn() {
        return playedOn;
    }

    public MeleeFormat getFormat() {
        return format;
    }

    public int getTeamSize() {
        return teamSize;
    }

    public MeleeStatus getStatus() {
        return status;
    }

    public String getPublicCode() {
        return publicCode;
    }

    public MeleeSettings getSettings() {
        return settings;
    }

    public ScoringTable getScoring() {
        return scoring;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }
}
