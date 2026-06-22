package com.dnd.app.repository;

import com.dnd.app.domain.EquipmentSlot;
import com.dnd.app.domain.ItemInstance;
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

    /**
     * Finds a non-unique, unequipped stack on a character matching the given item source
     * (exactly one of template / equipment / magic id is non-null). Used to merge stackable grants.
     */
    @Query("select i from ItemInstance i where i.ownerCharacter.id = :charId and i.slot is null and i.isUnique = false " +
            "and ((:tpl is not null and i.template.id = :tpl) " +
            "  or (:eq is not null and i.equipmentItem.id = :eq) " +
            "  or (:mg is not null and i.magicItem.id = :mg))")
    Optional<ItemInstance> findStackableForCharacter(@Param("charId") UUID characterId,
                                                     @Param("tpl") UUID templateId,
                                                     @Param("eq") UUID equipmentItemId,
                                                     @Param("mg") UUID magicItemId);

    /** As {@link #findStackableForCharacter} but for a shared-storage stack. */
    @Query("select i from ItemInstance i where i.sharedStorage.id = :storageId and i.isUnique = false " +
            "and ((:tpl is not null and i.template.id = :tpl) " +
            "  or (:eq is not null and i.equipmentItem.id = :eq) " +
            "  or (:mg is not null and i.magicItem.id = :mg))")
    Optional<ItemInstance> findStackableForStorage(@Param("storageId") UUID storageId,
                                                   @Param("tpl") UUID templateId,
                                                   @Param("eq") UUID equipmentItemId,
                                                   @Param("mg") UUID magicItemId);

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
