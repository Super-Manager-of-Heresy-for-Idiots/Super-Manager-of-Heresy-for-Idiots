package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureEffectDefinitionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureEffectDefinitionRepository extends JpaRepository<FeatureEffectDefinition, UUID> {
    List<FeatureEffectDefinition> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureEffectDefinition> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
}
