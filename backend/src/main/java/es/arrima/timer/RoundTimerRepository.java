package es.arrima.timer;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoundTimerRepository extends JpaRepository<RoundTimer, Long> {

    List<RoundTimer> findByMeleeIdOrderByRoundNumber(long meleeId);

    Optional<RoundTimer> findByMeleeIdAndRoundNumber(long meleeId, int roundNumber);

    /** Countdowns not over yet, of every melee: few rows (one per melee being played). */
    List<RoundTimer> findByEndedAtIsNull();

    /**
     * Records that the time ran out, only if nobody did it first and nothing changed since it was
     * read (a pause or resume changes pausedMillis or pausedAt). Exactly one caller gets 1 back:
     * that one sends the notifications, so they go out once even if several triggers fire together.
     */
    @Modifying
    @Query("""
            update RoundTimer t set t.endedAt = :endedAt, t.endReason = es.arrima.timer.EndReason.TIME_UP
            where t.id = :id and t.endedAt is null and t.pausedAt is null and t.pausedMillis = :pausedMillis""")
    int markTimeUp(@Param("id") long id, @Param("pausedMillis") long pausedMillis, @Param("endedAt") Instant endedAt);

    @Modifying
    @Query("delete from RoundTimer t where t.meleeId = :meleeId")
    int deleteByMeleeId(@Param("meleeId") long meleeId);
}
