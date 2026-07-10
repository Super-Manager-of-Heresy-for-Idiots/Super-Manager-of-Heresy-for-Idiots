package com.dnd.app.repository;

import com.dnd.app.domain.ItemEnchantment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт ItemEnchantmentRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface ItemEnchantmentRepository extends JpaRepository<ItemEnchantment, UUID> {

    List<ItemEnchantment> findByItemInstanceId(UUID instanceId);

    boolean existsByItemInstanceIdAndEnchantmentTypeId(UUID instanceId, UUID enchantmentTypeId);

    long countByEnchantmentTypeId(UUID enchantmentTypeId);
}
