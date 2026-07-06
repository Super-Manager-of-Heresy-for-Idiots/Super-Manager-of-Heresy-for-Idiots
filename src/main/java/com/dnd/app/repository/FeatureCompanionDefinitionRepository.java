package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureCompanionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeatureCompanionDefinitionRepository extends JpaRepository<FeatureCompanionDefinition, UUID> {
    List<FeatureCompanionDefinition> findByFeatureRuleIdOrderBySortOrderAsc(UUID featureRuleId);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
