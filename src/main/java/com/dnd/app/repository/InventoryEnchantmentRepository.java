package com.dnd.app.repository;

import com.dnd.app.domain.InventoryEnchantment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InventoryEnchantmentRepository extends JpaRepository<InventoryEnchantment, UUID> {

    List<InventoryEnchantment> findAllByInventorySlotId(UUID inventorySlotId);

    boolean existsByInventorySlotIdAndEnchantmentTypeId(UUID inventorySlotId, UUID enchantmentTypeId);

    long countByEnchantmentTypeId(UUID enchantmentTypeId);
}
