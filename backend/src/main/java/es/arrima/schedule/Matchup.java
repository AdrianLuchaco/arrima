package es.arrima.schedule;

import es.arrima.schedule.domain.Pairing;
import es.arrima.shared.error.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** One match of the court schedule: round, court (null while waiting), the two teams and the winner. */
@Entity
@Table(name = "matchup")
public class Matchup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private int roundNumber;

    // Explicit names: the default naming strategy would turn "teamAId" into "teamaid".
    @Column(name = "team_a_id")
    private long teamAId;

    @Column(name = "team_b_id")
    private long teamBId;

    private Integer courtNumber;

    private Long winnerTeamId;

    private Instant decidedAt;

    protected Matchup() {
        // for JPA
    }

    Matchup(long meleeId, int roundNumber, Pairing pairing, Integer courtNumber) {
        this.meleeId = meleeId;
        this.roundNumber = roundNumber;
        this.teamAId = pairing.teamA();
        this.teamBId = pairing.teamB();
        this.courtNumber = courtNumber;
    }

    /** Sets (or corrects) the winner; null clears the result. Recording the same winner twice is harmless. */
    void decide(Long winnerTeamId, Instant now) {
        if (winnerTeamId != null && winnerTeamId != teamAId && winnerTeamId != teamBId) {
            throw ApiException.validation("winnerTeamId", "NotInMatch");
        }
        this.winnerTeamId = winnerTeamId;
        this.decidedAt = winnerTeamId == null ? null : now;
    }

    void moveToCourt(int courtNumber) {
        this.courtNumber = courtNumber;
    }

    public boolean isDecided() {
        return winnerTeamId != null;
    }

    public Long getId() {
        return id;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public long getTeamAId() {
        return teamAId;
    }

    public long getTeamBId() {
        return teamBId;
    }

    public Integer getCourtNumber() {
        return courtNumber;
    }

    public Long getWinnerTeamId() {
        return winnerTeamId;
    }
}
