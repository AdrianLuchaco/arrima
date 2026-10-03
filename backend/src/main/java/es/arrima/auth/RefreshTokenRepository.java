package es.arrima.auth;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Locks the row (SELECT ... FOR UPDATE) so two simultaneous refreshes with the same token are
     * processed one after the other instead of both rotating it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.adminId = :adminId and t.revokedAt is null")
    int revokeAllOfAdmin(@Param("adminId") long adminId, @Param("now") Instant now);

    /** Housekeeping: expired tokens are useless, even for detecting reuse. */
    @Modifying
    @Query("delete from RefreshToken t where t.adminId = :adminId and t.expiresAt < :now")
    int deleteExpiredOfAdmin(@Param("adminId") long adminId, @Param("now") Instant now);
}
