package com.dnd.app.repository;

import com.dnd.app.domain.EnchantmentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Контракт EnchantmentTypeRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface EnchantmentTypeRepository extends JpaRepository<EnchantmentType, UUID> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, UUID id);

    long countByBuffDebuffId(UUID buffDebuffId);
}
