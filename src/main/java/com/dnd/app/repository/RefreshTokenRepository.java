package com.dnd.app.repository;

import com.dnd.app.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByJti(UUID jti);

    /** Revokes every still-active token in a rotation family (theft response / logout). */
    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.familyId = :familyId and r.revoked = false")
    int revokeFamily(@Param("familyId") UUID familyId);

    /** Bulk-purges expired rows; run from the scheduled cleanup task. */
    @Modifying
    @Query("delete from RefreshToken r where r.expiresAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
