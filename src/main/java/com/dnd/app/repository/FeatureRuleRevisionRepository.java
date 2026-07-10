package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureRuleRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт FeatureRuleRevisionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureRuleRevisionRepository extends JpaRepository<FeatureRuleRevision, UUID> {

    List<FeatureRuleRevision> findByFeatureRuleIdOrderByRevisionNumberDesc(UUID featureRuleId);

    Optional<FeatureRuleRevision> findTopByFeatureRuleIdOrderByRevisionNumberDesc(UUID featureRuleId);
}
