package es.arrima.international;

import es.arrima.international.domain.ThrowKind;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BallThrowRepository extends JpaRepository<BallThrow, Long> {

    List<BallThrow> findByRoundIdIn(Collection<Long> roundIds);

    Optional<BallThrow> findByRoundIdAndTeamIdAndKindAndBallNumber(long roundId, long teamId, ThrowKind kind, int ballNumber);

    @Query("""
            select count(b) from BallThrow b where b.roundId in
              (select r.id from InternationalRound r where r.groupId in
                (select g.id from InternationalGroup g where g.meleeId = :meleeId))""")
    long countByMeleeId(@Param("meleeId") long meleeId);
}
