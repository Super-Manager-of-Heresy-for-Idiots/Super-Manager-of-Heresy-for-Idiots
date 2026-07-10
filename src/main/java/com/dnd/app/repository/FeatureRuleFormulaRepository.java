package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureRuleFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт FeatureRuleFormulaRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface FeatureRuleFormulaRepository extends JpaRepository<FeatureRuleFormula, UUID> {
    List<FeatureRuleFormula> findByFeatureRuleIdOrderBySortOrderAsc(UUID featureRuleId);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
