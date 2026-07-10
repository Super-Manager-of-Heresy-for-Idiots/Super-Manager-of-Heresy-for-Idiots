package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureChoiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureChoiceGroupRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureChoiceGroupRepository extends JpaRepository<FeatureChoiceGroup, UUID> {
    List<FeatureChoiceGroup> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureChoiceGroup> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
