package es.arrima.auth;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SignupInvitationRepository extends JpaRepository<SignupInvitation, Long> {

    /** Locked, so the same code cannot register two clubs at the same instant. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SignupInvitation> findByCodeHash(String codeHash);
}
