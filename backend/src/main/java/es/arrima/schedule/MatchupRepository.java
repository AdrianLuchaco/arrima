package es.arrima.schedule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchupRepository extends JpaRepository<Matchup, Long> {

    /** In grid order: round by round, by court, and the waiting ones (no court) at the end of each round. */
    List<Matchup> findByMeleeIdOrderByRoundNumberAscCourtNumberAscIdAsc(long meleeId);

    Optional<Matchup> findByIdAndMeleeId(long id, long meleeId);

    boolean existsByMeleeId(long meleeId);

    long countByMeleeIdAndWinnerTeamIdIsNotNull(long meleeId);
}
