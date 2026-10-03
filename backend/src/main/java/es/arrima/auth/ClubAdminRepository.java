package es.arrima.auth;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface ClubAdminRepository extends JpaRepository<ClubAdmin, Long> {

    /** E-mails are stored in lower case: callers must normalise before searching. */
    Optional<ClubAdmin> findByEmail(String email);

    boolean existsByEmail(String email);
}
