package com.dnd.app.repository;

import com.dnd.app.domain.content.EquipmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт EquipmentCategoryRepository — доступ к категориям снаряжения (equipment_category).
 * Используется авторингом homebrew-снаряжения для резолвинга категории по slug (IT-2).
 */
public interface EquipmentCategoryRepository extends JpaRepository<EquipmentCategory, UUID> {

    /** Ванильная категория по slug. */
    Optional<EquipmentCategory> findBySlugAndHomebrewIsNull(String slug);

    /** Категория, принадлежащая конкретному пакету, по slug. */
    Optional<EquipmentCategory> findBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    /** Ванильные категории для справочника FE (сортировка по русскому имени). */
    List<EquipmentCategory> findByHomebrewIsNullOrderByNameRuAsc();
}
