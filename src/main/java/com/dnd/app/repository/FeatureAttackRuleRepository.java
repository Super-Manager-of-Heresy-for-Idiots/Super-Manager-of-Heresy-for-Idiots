package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureAttackRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureAttackRuleRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureAttackRuleRepository extends JpaRepository<FeatureAttackRule, UUID> {
    List<FeatureAttackRule> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
}
