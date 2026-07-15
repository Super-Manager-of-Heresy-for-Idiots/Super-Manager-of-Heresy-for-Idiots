package com.dnd.app.repository;

import com.dnd.app.domain.BattleCommandIdempotencyRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleCommandIdempotencyRepository extends JpaRepository<BattleCommandIdempotencyRecord, UUID> {

    boolean existsByClientCommandId(UUID clientCommandId);

    Optional<BattleCommandIdempotencyRecord> findByClientCommandId(UUID clientCommandId);

    @Modifying
    @Query("delete from BattleCommandIdempotencyRecord r where r.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
