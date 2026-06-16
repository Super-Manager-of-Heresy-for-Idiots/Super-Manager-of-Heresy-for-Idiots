package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantAbilityScoreRepository extends JpaRepository<ClassLevelRewardGrantAbilityScore, UUID> {

    List<ClassLevelRewardGrantAbilityScore> findAllByFixedAbilityScoreId(UUID abilityScoreId);
}
