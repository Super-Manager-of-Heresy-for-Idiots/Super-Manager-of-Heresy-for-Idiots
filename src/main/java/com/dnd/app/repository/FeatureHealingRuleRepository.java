package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureHealingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureHealingRuleRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureHealingRuleRepository extends JpaRepository<FeatureHealingRule, UUID> {
    List<FeatureHealingRule> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);

    List<FeatureHealingRule> findByFeatureRuleId(UUID featureRuleId);
}
