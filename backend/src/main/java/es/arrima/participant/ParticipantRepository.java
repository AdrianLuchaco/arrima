package es.arrima.participant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {

    /** In list order; people added by hand without a number go last. */
    @Query("select p from Participant p where p.meleeId = :meleeId order by p.listNumber asc nulls last, p.id asc")
    List<Participant> findByMeleeIdInListOrder(@Param("meleeId") long meleeId);

    Optional<Participant> findByIdAndMeleeId(long id, long meleeId);

    long countByMeleeId(long meleeId);

    /** Active players per melee, for the melee list (one query for all melees). */
    @Query("""
            select p.meleeId as meleeId, count(p) as players from Participant p
            where p.meleeId in :meleeIds and p.status = es.arrima.participant.ParticipantStatus.ACTIVE
            group by p.meleeId""")
    List<ActiveCount> countActiveByMeleeIds(@Param("meleeIds") Collection<Long> meleeIds);

    interface ActiveCount {
        long getMeleeId();

        long getPlayers();
    }
}
