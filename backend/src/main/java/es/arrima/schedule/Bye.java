package es.arrima.schedule;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The team that rests in a round (odd number of teams). It counts as a win. */
@Entity
@Table(name = "bye")
public class Bye {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private int roundNumber;

    private long teamId;

    protected Bye() {
        // for JPA
    }

    Bye(long meleeId, int roundNumber, long teamId) {
        this.meleeId = meleeId;
        this.roundNumber = roundNumber;
        this.teamId = teamId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public long getTeamId() {
        return teamId;
    }
}
