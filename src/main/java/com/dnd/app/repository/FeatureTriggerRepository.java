package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureTrigger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureTriggerRepository extends JpaRepository<FeatureTrigger, UUID> {
    List<FeatureTrigger> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
}
