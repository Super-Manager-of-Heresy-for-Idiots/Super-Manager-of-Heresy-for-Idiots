package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureChoiceOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FeatureChoiceOptionRepository extends JpaRepository<FeatureChoiceOption, UUID> {
    List<FeatureChoiceOption> findByChoiceGroupIdOrderBySortOrderAsc(UUID choiceGroupId);
    List<FeatureChoiceOption> findByChoiceGroupIdIn(Collection<UUID> choiceGroupIds);
}
