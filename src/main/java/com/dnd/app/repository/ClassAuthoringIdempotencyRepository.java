package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassAuthoringIdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ClassAuthoringIdempotencyRepository extends JpaRepository<ClassAuthoringIdempotencyRecord, UUID> {

    Optional<ClassAuthoringIdempotencyRecord> findByScopeAndIdemKey(String scope, String idemKey);

    @Modifying
    @Query("delete from ClassAuthoringIdempotencyRecord r where r.createdAt < :cutoff")
    int deleteByCreatedAtBefore(@Param("cutoff") Instant cutoff);
}
