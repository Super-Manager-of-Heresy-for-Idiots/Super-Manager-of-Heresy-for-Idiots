package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureChoiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureChoiceGroupRepository extends JpaRepository<FeatureChoiceGroup, UUID> {
    List<FeatureChoiceGroup> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureChoiceGroup> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
