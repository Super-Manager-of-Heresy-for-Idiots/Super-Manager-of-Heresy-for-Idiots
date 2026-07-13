package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureItemBinding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий структурного binding item-правил.
 */
public interface FeatureItemBindingRepository extends JpaRepository<FeatureItemBinding, UUID> {
    Optional<FeatureItemBinding> findByFeatureRuleId(UUID featureRuleId);

    List<FeatureItemBinding> findByFeatureRuleIdIn(Collection<UUID> featureRuleIds);
}
