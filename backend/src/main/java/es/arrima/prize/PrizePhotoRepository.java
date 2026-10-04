package es.arrima.prize;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrizePhotoRepository extends JpaRepository<PrizePhoto, Long> {

    List<PrizePhoto> findByPrizeIdInOrderById(Collection<Long> prizeIds);

    Optional<PrizePhoto> findByIdAndPrizeId(long id, long prizeId);

    long countByPrizeId(long prizeId);

    @Query("select count(p) from PrizePhoto p where p.prizeId in (select z.id from Prize z where z.meleeId = :meleeId)")
    long countByMeleeId(@Param("meleeId") long meleeId);
}
