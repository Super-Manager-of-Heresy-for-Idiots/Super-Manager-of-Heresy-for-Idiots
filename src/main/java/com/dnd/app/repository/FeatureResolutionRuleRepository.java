package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureResolutionRuleRepository extends JpaRepository<FeatureResolutionRule, UUID> {
    List<FeatureResolutionRule> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
    List<FeatureResolutionRule> findByFeatureRuleId(UUID featureRuleId);
    Optional<FeatureResolutionRule> findById(UUID id);
}
