package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureHealingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureHealingRuleRepository extends JpaRepository<FeatureHealingRule, UUID> {
    List<FeatureHealingRule> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);

    List<FeatureHealingRule> findByFeatureRuleId(UUID featureRuleId);
}
