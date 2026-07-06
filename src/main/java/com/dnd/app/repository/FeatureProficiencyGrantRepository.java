package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureProficiencyGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureProficiencyGrantRepository extends JpaRepository<FeatureProficiencyGrant, UUID> {
    List<FeatureProficiencyGrant> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureProficiencyGrant> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
