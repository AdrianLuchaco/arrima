package es.arrima.international;

import es.arrima.international.domain.ThrowKind;
import es.arrima.international.domain.ThrowOutcome;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One ball of la Internacional: team, kind, number (1-3), result and its points. Every ball is kept,
 * so a team that complains can see its six balls and the admin can correct any of them.
 */
@Entity
@Table(name = "ball_throw")
public class BallThrow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roundId;

    private long teamId;

    @Enumerated(EnumType.STRING)
    private ThrowKind kind;

    private int ballNumber;

    @Enumerated(EnumType.STRING)
    private ThrowOutcome outcome;

    private int points;

    private Instant recordedAt;

    private Instant correctedAt;

    protected BallThrow() {
        // for JPA
    }

    BallThrow(long roundId, long teamId, ThrowKind kind, int ballNumber, ThrowOutcome outcome, int points, Instant now) {
        this.roundId = roundId;
        this.teamId = teamId;
        this.kind = kind;
        this.ballNumber = ballNumber;
        this.outcome = outcome;
        this.points = points;
        this.recordedAt = now;
    }

    /** Recording the same outcome again (an offline retry) is not a correction. */
    void correct(ThrowOutcome outcome, int points, Instant now) {
        if (outcome != this.outcome) {
            this.outcome = outcome;
            this.points = points;
            this.correctedAt = now;
        }
    }

    public long getRoundId() {
        return roundId;
    }

    public long getTeamId() {
        return teamId;
    }

    public ThrowKind getKind() {
        return kind;
    }

    public int getBallNumber() {
        return ballNumber;
    }

    public ThrowOutcome getOutcome() {
        return outcome;
    }

    public int getPoints() {
        return points;
    }

    public boolean isCorrected() {
        return correctedAt != null;
    }
}
