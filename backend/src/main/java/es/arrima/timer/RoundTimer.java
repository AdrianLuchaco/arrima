package es.arrima.timer;

import es.arrima.timer.domain.Countdown;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/** The countdown of one round of a melee, as stored (see Countdown for what it means). */
@Entity
@Table(name = "round_timer")
public class RoundTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private int roundNumber;

    private int durationSeconds;

    private Instant startedAt;

    private long pausedMillis;

    private Instant pausedAt;

    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    private EndReason endReason;

    protected RoundTimer() {
        // for JPA
    }

    RoundTimer(long meleeId, int roundNumber, Duration duration, Instant now) {
        this.meleeId = meleeId;
        this.roundNumber = roundNumber;
        this.durationSeconds = (int) duration.toSeconds();
        this.startedAt = now;
    }

    public Countdown countdown() {
        return new Countdown(startedAt, Duration.ofSeconds(durationSeconds), Duration.ofMillis(pausedMillis), pausedAt, endedAt);
    }

    void pause(Instant now) {
        apply(countdown().pause(now));
    }

    void resume(Instant now) {
        apply(countdown().resume(now));
    }

    /** The next round started while this one still had time: no alarm. */
    void stopForNextRound(Instant now) {
        apply(countdown().end(now));
        this.endReason = EndReason.NEXT_ROUND;
    }

    private void apply(Countdown countdown) {
        this.pausedMillis = countdown.pausedBefore().toMillis();
        this.pausedAt = countdown.pausedAt();
        this.endedAt = countdown.endedAt();
    }

    public Long getId() {
        return id;
    }

    public long getMeleeId() {
        return meleeId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public long getPausedMillis() {
        return pausedMillis;
    }

    public EndReason getEndReason() {
        return endReason;
    }
}
