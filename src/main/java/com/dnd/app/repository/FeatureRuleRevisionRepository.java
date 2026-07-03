package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureRuleRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeatureRuleRevisionRepository extends JpaRepository<FeatureRuleRevision, UUID> {

    List<FeatureRuleRevision> findByFeatureRuleIdOrderByRevisionNumberDesc(UUID featureRuleId);

    Optional<FeatureRuleRevision> findTopByFeatureRuleIdOrderByRevisionNumberDesc(UUID featureRuleId);
}
