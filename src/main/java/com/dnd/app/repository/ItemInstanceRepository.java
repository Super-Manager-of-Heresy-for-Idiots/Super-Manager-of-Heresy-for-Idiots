package com.dnd.app.repository;

import com.dnd.app.domain.ItemInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemInstanceRepository extends JpaRepository<ItemInstance, UUID> {

    List<ItemInstance> findByOwnerCharacterId(UUID characterId);

    List<ItemInstance> findByOwnerCharacterIdAndSlotIsNotNull(UUID characterId);

    List<ItemInstance> findByOwnerCharacterIdAndSlotIsNull(UUID characterId);

    List<ItemInstance> findBySharedStorageId(UUID storageId);

    Optional<ItemInstance> findByOwnerCharacterIdAndTemplateIdAndSlotIsNullAndIsUniqueFalse(UUID characterId, UUID templateId);
}
