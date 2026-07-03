package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureRuleRepository extends JpaRepository<FeatureRule, UUID> {

    List<FeatureRule> findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(String ownerType, UUID ownerId);

    List<FeatureRule> findByOwnerType(String ownerType);

    List<FeatureRule> findByOwnerTypeAndOwnerIdIn(String ownerType, Collection<UUID> ownerIds);

    long countByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
}
