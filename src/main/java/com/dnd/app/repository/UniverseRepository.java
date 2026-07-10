package com.dnd.app.repository;

import com.dnd.app.domain.Universe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт UniverseRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface UniverseRepository extends JpaRepository<Universe, UUID> {

    Optional<Universe> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Universe> findAllByOrderByNameAsc();
}
