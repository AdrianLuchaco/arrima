package es.arrima.push;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    List<PushSubscription> findByMeleeId(long meleeId);

    Optional<PushSubscription> findByMeleeIdAndEndpoint(long meleeId, String endpoint);

    long countByMeleeId(long meleeId);

    @Modifying
    @Query("delete from PushSubscription s where s.meleeId = :meleeId")
    int deleteByMeleeId(@Param("meleeId") long meleeId);
}
