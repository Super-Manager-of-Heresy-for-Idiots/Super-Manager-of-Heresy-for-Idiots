package com.dnd.app.repository;

import com.dnd.app.domain.content.SpeciesTraitEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Контракт SpeciesTraitEffectRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface SpeciesTraitEffectRepository extends JpaRepository<SpeciesTraitEffect, UUID> {

    /** Trait effects of a given type (e.g. {@code resistance}) for a species, across all its traits. */
    @Query("select e from SpeciesTraitEffect e "
            + "where e.trait.species.id = :speciesId and e.effectType = :effectType")
    List<SpeciesTraitEffect> findBySpeciesIdAndEffectType(@Param("speciesId") UUID speciesId,
                                                          @Param("effectType") String effectType);
}
