package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureLanguageGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureLanguageGrantRepository extends JpaRepository<FeatureLanguageGrant, UUID> {
    List<FeatureLanguageGrant> findByFeatureRuleId(UUID featureRuleId);
    List<FeatureLanguageGrant> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
    void deleteByFeatureRuleId(UUID featureRuleId);
}
