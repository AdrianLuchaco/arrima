package es.arrima.schedule;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ByeRepository extends JpaRepository<Bye, Long> {

    List<Bye> findByMeleeIdOrderByRoundNumber(long meleeId);
}
