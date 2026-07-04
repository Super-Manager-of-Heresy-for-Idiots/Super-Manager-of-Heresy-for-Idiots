package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureEffectEndCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureEffectEndConditionRepository extends JpaRepository<FeatureEffectEndCondition, UUID> {
    List<FeatureEffectEndCondition> findByEffectDefinitionId(UUID effectDefinitionId);
    List<FeatureEffectEndCondition> findByEffectDefinitionIdIn(Collection<UUID> effectDefinitionIds);
}
