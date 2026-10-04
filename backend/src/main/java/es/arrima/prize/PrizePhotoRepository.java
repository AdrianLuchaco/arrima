package es.arrima.prize;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrizePhotoRepository extends JpaRepository<PrizePhoto, Long> {

    List<PrizePhoto> findByPrizeIdInOrderById(Collection<Long> prizeIds);

    Optional<PrizePhoto> findByIdAndPrizeId(long id, long prizeId);

    long countByPrizeId(long prizeId);

    @Query("select count(p) from PrizePhoto p where p.prizeId in (select z.id from Prize z where z.meleeId = :meleeId)")
    long countByMeleeId(@Param("meleeId") long meleeId);

    /**
     * Two statements, in this order: the database allows one main photo per prize and checks it row
     * by row, so the old one must stop being main before the new one becomes it.
     */
    @Modifying
    @Query("update PrizePhoto p set p.main = false where p.prizeId = :prizeId and p.main = true")
    int clearMain(@Param("prizeId") long prizeId);

    @Modifying
    @Query("update PrizePhoto p set p.main = true where p.id = :photoId")
    int markMain(@Param("photoId") long photoId);
}
