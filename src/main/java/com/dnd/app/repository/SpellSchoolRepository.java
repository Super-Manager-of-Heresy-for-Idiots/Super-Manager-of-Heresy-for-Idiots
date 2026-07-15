package com.dnd.app.repository;

import com.dnd.app.domain.SpellSchool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт SpellSchoolRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface SpellSchoolRepository extends JpaRepository<SpellSchool, UUID> {

    List<SpellSchool> findAllByOrderByNameRuAsc();

    /** Школа магии по slug (резолвинг при авторинге заклинания, P2-1). */
    Optional<SpellSchool> findBySlug(String slug);
}
