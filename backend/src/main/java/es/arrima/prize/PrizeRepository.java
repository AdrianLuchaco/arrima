package es.arrima.prize;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrizeRepository extends JpaRepository<Prize, Long> {

    List<Prize> findByMeleeIdOrderByPosition(long meleeId);

    Optional<Prize> findByIdAndMeleeId(long id, long meleeId);
}
