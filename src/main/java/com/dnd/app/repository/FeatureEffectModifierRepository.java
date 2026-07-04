package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureEffectModifierRepository extends JpaRepository<FeatureEffectModifier, UUID> {
    List<FeatureEffectModifier> findByEffectDefinitionId(UUID effectDefinitionId);
    List<FeatureEffectModifier> findByEffectDefinitionIdIn(Collection<UUID> effectDefinitionIds);
}
