package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantFeature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantFeatureRepository extends JpaRepository<ClassLevelRewardGrantFeature, UUID> {

    List<ClassLevelRewardGrantFeature> findAllByClassFeatureId(UUID classFeatureId);
}
