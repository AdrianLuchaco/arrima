package es.arrima.team;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

    /** Teams with their members in one query (instead of one extra query per team). */
    @Query("select distinct t from Team t left join fetch t.memberIds where t.meleeId = :meleeId order by t.number")
    List<Team> findByMeleeIdWithMembers(@Param("meleeId") long meleeId);

    Optional<Team> findByIdAndMeleeId(long id, long meleeId);

    boolean existsByMeleeId(long meleeId);
}
