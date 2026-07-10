package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureDamageRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureDamageRuleRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureDamageRuleRepository extends JpaRepository<FeatureDamageRule, UUID> {
    List<FeatureDamageRule> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);

    List<FeatureDamageRule> findByFeatureRuleId(UUID featureRuleId);
}
