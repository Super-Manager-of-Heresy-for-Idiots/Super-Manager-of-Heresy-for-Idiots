package com.dnd.app.repository;

import com.dnd.app.domain.BagSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BagSlotRepository extends JpaRepository<BagSlot, UUID> {

    List<BagSlot> findAllByCharacterId(UUID characterId);

    Optional<BagSlot> findByCharacterIdAndItemTypeId(UUID characterId, UUID itemTypeId);

    Optional<BagSlot> findByCharacterIdAndArtifactId(UUID characterId, UUID artifactId);
}
