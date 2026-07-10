package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureEffectModifierRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureEffectModifierRepository extends JpaRepository<FeatureEffectModifier, UUID> {
    List<FeatureEffectModifier> findByEffectDefinitionId(UUID effectDefinitionId);
    List<FeatureEffectModifier> findByEffectDefinitionIdIn(Collection<UUID> effectDefinitionIds);
}
