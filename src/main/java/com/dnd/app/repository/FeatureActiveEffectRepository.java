package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureActiveEffectRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureActiveEffectRepository extends JpaRepository<FeatureActiveEffect, UUID> {

    List<FeatureActiveEffect> findByCharacterIdAndStatus(UUID characterId, String status);

    /** Effects created BY this caster (concentration is keyed by the caster, not the effect's holder). */
    List<FeatureActiveEffect> findBySourceCharacterIdAndStatus(UUID sourceCharacterId, String status);

    List<FeatureActiveEffect> findByCharacterId(UUID characterId);

    List<FeatureActiveEffect> findByCharacterIdAndEffectDefinitionIdAndStatus(
            UUID characterId, UUID effectDefinitionId, String status);

    /** Effects that are still active but whose wall-clock expiry has passed (cleanup / expiration sweep). */
    List<FeatureActiveEffect> findByStatusAndExpiresAtIsNotNullAndExpiresAtBefore(String status, Instant cutoff);
}
