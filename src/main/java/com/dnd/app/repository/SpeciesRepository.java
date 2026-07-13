package com.dnd.app.repository;

import com.dnd.app.domain.content.Species;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Контракт SpeciesRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface SpeciesRepository extends JpaRepository<Species, UUID> {

    Optional<Species> findBySlugAndHomebrewIsNull(String slug);

    boolean existsBySlugAndHomebrewIsNull(String slug);

    List<Species> findAllByHomebrewIsNull();

    List<Species> findAllByHomebrewIdIn(Set<UUID> homebrewIds);

    // --- SP-1: авторинг видов в пакете ---

    Optional<Species> findByIdAndHomebrew_Id(UUID id, UUID homebrewId);

    boolean existsBySlugAndHomebrew_Id(String slug, UUID homebrewId);

    List<Species> findAllByHomebrew_Id(UUID homebrewId);
}
