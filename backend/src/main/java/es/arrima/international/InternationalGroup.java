package es.arrima.international;

import es.arrima.international.domain.PlannedGroup;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A group of teams with the same wins that plays la Internacional for some prize positions. */
@Entity
@Table(name = "international_group")
public class InternationalGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private int playOrder;

    private int wins;

    private int bestPrizePosition;

    private int worstPrizePosition;

    @Enumerated(EnumType.STRING)
    private GroupStatus status;

    protected InternationalGroup() {
        // for JPA
    }

    InternationalGroup(long meleeId, PlannedGroup plan, int playOrder) {
        this.meleeId = meleeId;
        this.status = GroupStatus.PENDING;
        update(plan, playOrder);
    }

    /** After results changed (going back and forward again), the same teams may compete for other prizes. */
    void update(PlannedGroup plan, int playOrder) {
        this.playOrder = playOrder;
        this.wins = plan.wins();
        this.bestPrizePosition = plan.bestPosition();
        this.worstPrizePosition = plan.worstPosition();
    }

    void changeStatus(GroupStatus status) {
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public long getMeleeId() {
        return meleeId;
    }

    public int getPlayOrder() {
        return playOrder;
    }

    public int getWins() {
        return wins;
    }

    public int getBestPrizePosition() {
        return bestPrizePosition;
    }

    public int getWorstPrizePosition() {
        return worstPrizePosition;
    }

    public GroupStatus getStatus() {
        return status;
    }

    public enum GroupStatus {
        PENDING,
        IN_PROGRESS,
        FINISHED
    }
}
