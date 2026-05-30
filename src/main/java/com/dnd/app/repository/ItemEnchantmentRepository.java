package com.dnd.app.repository;

import com.dnd.app.domain.ItemEnchantment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemEnchantmentRepository extends JpaRepository<ItemEnchantment, UUID> {

    List<ItemEnchantment> findByItemInstanceId(UUID instanceId);

    boolean existsByItemInstanceIdAndEnchantmentTypeId(UUID instanceId, UUID enchantmentTypeId);
}
