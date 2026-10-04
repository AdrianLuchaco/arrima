package es.arrima.prize;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A prize position and the team that won it, with la Internacional's points if it played. */
@Entity
@Table(name = "prize")
public class Prize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private int position;

    private long teamId;

    private Integer internationalPoints;

    private Instant awardedAt;

    protected Prize() {
        // for JPA
    }

    Prize(long meleeId, int position, long teamId, Integer internationalPoints) {
        this.meleeId = meleeId;
        this.position = position;
        this.teamId = teamId;
        this.internationalPoints = internationalPoints;
    }

    /** The same team after recalculating: if its position changed, it has not been handed out yet. */
    void moveTo(int position, Integer internationalPoints) {
        if (position != this.position) {
            this.awardedAt = null;
        }
        this.position = position;
        this.internationalPoints = internationalPoints;
    }

    void markAwarded(Instant now) {
        if (awardedAt == null) {
            this.awardedAt = now;
        }
    }

    public boolean isAwarded() {
        return awardedAt != null;
    }

    public Long getId() {
        return id;
    }

    public long getMeleeId() {
        return meleeId;
    }

    public int getPosition() {
        return position;
    }

    public long getTeamId() {
        return teamId;
    }

    public Integer getInternationalPoints() {
        return internationalPoints;
    }
}
