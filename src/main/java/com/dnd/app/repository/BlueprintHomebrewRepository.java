package com.dnd.app.repository;

import com.dnd.app.domain.BlueprintHomebrew;
import com.dnd.app.domain.BlueprintHomebrewId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BlueprintHomebrewRepository extends JpaRepository<BlueprintHomebrew, BlueprintHomebrewId> {

    List<BlueprintHomebrew> findByBlueprintId(UUID blueprintId);

    boolean existsByBlueprintIdAndPackageId(UUID blueprintId, UUID packageId);

    void deleteByBlueprintIdAndPackageId(UUID blueprintId, UUID packageId);

    long countByBlueprintId(UUID blueprintId);
}
