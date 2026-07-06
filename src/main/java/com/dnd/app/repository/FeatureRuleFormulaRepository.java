package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureRuleFormula;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeatureRuleFormulaRepository extends JpaRepository<FeatureRuleFormula, UUID> {
    List<FeatureRuleFormula> findByFeatureRuleIdOrderBySortOrderAsc(UUID featureRuleId);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
