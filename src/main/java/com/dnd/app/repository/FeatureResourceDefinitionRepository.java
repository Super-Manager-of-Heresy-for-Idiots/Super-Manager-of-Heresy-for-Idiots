package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureResourceDefinitionRepository extends JpaRepository<FeatureResourceDefinition, UUID> {
    List<FeatureResourceDefinition> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureResourceDefinition> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
}
