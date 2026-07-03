package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureRuleIssue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureRuleIssueRepository extends JpaRepository<FeatureRuleIssue, UUID> {

    List<FeatureRuleIssue> findByOwnerTypeAndOwnerIdOrderByResolvedAscCreatedAtDesc(String ownerType, UUID ownerId);

    List<FeatureRuleIssue> findByOwnerType(String ownerType);

    List<FeatureRuleIssue> findByOwnerTypeAndOwnerIdIn(String ownerType, Collection<UUID> ownerIds);

    List<FeatureRuleIssue> findByFeatureRuleId(UUID featureRuleId);

    /** Approval gate: a rule cannot be approved while it has an unresolved issue of the given severity. */
    boolean existsByFeatureRuleIdAndResolvedFalseAndSeverity(UUID featureRuleId, String severity);
}
