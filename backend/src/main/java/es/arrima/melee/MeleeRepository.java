package es.arrima.melee;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeleeRepository extends JpaRepository<Melee, Long> {

    Optional<Melee> findByIdAndClubId(long id, long clubId);

    Optional<Melee> findByPublicCode(String publicCode);

    boolean existsByPublicCode(String publicCode);

    List<Melee> findByClubIdOrderByPlayedOnDescIdDesc(long clubId);

    /**
     * Atomic increment, so two changes at the same time (two phones of the same club) both count.
     * Pending entity changes are flushed first, so they are part of the new revision.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Melee m set m.revision = m.revision + 1, m.lastActivityAt = :now where m.id = :id")
    void recordChange(@Param("id") long id, @Param("now") Instant now);

    @Query("select m.revision from Melee m where m.id = :id")
    long findRevision(@Param("id") long id);
}
