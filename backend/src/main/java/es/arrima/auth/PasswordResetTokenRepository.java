package es.arrima.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    /** Locked, so the same link opened twice at once changes the password only once. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Only the latest link works: asking for a new one cancels the previous. */
    @Modifying
    @Query("delete from PasswordResetToken t where t.adminId = :adminId")
    int deleteAllOfAdmin(@Param("adminId") long adminId);

    /** Housekeeping: expired links are useless. */
    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
