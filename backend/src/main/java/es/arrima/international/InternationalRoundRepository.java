package es.arrima.international;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InternationalRoundRepository extends JpaRepository<InternationalRound, Long> {

    /** Rounds with their teams in one query, in creation order within each group. */
    @Query("""
            select distinct r from InternationalRound r left join fetch r.teams
            where r.groupId in :groupIds order by r.groupId, r.roundNumber""")
    List<InternationalRound> findByGroupIdsWithTeams(@Param("groupIds") Collection<Long> groupIds);
}
