package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureSpellGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureSpellGrantRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureSpellGrantRepository extends JpaRepository<FeatureSpellGrant, UUID> {
    List<FeatureSpellGrant> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
    List<FeatureSpellGrant> findByFeatureRuleId(UUID featureRuleId);
}
