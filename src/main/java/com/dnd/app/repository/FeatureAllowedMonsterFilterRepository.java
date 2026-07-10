package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureAllowedMonsterFilter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureAllowedMonsterFilterRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureAllowedMonsterFilterRepository extends JpaRepository<FeatureAllowedMonsterFilter, UUID> {
    List<FeatureAllowedMonsterFilter> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
    List<FeatureAllowedMonsterFilter> findByFeatureRuleId(UUID featureRuleId);
}
