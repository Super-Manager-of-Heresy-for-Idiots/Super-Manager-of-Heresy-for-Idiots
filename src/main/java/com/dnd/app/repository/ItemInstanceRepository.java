package com.dnd.app.repository;

import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.enums.EquipmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ItemInstanceRepository extends JpaRepository<ItemInstance, UUID> {

    List<ItemInstance> findByOwnerCharacterId(UUID characterId);

    List<ItemInstance> findByOwnerCharacterIdAndSlotIsNotNull(UUID characterId);

    List<ItemInstance> findByOwnerCharacterIdAndSlotIsNull(UUID characterId);

    List<ItemInstance> findBySharedStorageId(UUID storageId);

    Optional<ItemInstance> findByOwnerCharacterIdAndTemplateIdAndSlotIsNullAndIsUniqueFalse(UUID characterId, UUID templateId);

    Optional<ItemInstance> findByOwnerCharacterIdAndSlot(UUID characterId, EquipmentSlot slot);

    /**
     * Atomic increment for stackable items to avoid lost updates under concurrent grants.
     * Returns rows updated (1 = success, 0 = no matching stackable row found).
     */
    @Modifying
    @Query("update ItemInstance i set i.quantity = i.quantity + :delta " +
            "where i.id = :instanceId")
    int incrementQuantity(@Param("instanceId") UUID instanceId, @Param("delta") int delta);
}
