package com.dnd.app.repository;

import com.dnd.app.domain.BlueprintNpc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlueprintNpcRepository extends JpaRepository<BlueprintNpc, UUID> {

    List<BlueprintNpc> findByBlueprintId(UUID blueprintId);

    Optional<BlueprintNpc> findByIdAndBlueprintId(UUID id, UUID blueprintId);
}
