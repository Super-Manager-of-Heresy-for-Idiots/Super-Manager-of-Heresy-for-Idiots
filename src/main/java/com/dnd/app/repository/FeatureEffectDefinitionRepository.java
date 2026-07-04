package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureEffectDefinitionRepository extends JpaRepository<FeatureEffectDefinition, UUID> {
    List<FeatureEffectDefinition> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureEffectDefinition> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
}
