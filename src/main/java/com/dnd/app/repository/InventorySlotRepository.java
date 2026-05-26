package com.dnd.app.repository;

import com.dnd.app.domain.InventorySlot;
import com.dnd.app.domain.enums.EquipmentSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventorySlotRepository extends JpaRepository<InventorySlot, UUID> {

    List<InventorySlot> findAllByCharacterId(UUID characterId);

    Optional<InventorySlot> findByCharacterIdAndSlot(UUID characterId, EquipmentSlot slot);
}
