package es.arrima.international;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InternationalGroupRepository extends JpaRepository<InternationalGroup, Long> {

    List<InternationalGroup> findByMeleeIdOrderByPlayOrder(long meleeId);
}
