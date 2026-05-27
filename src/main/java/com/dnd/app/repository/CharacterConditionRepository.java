package com.dnd.app.repository;

import com.dnd.app.domain.CharacterCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterConditionRepository extends JpaRepository<CharacterCondition, UUID> {

    List<CharacterCondition> findAllByCharacterIdAndActiveTrue(UUID characterId);

    boolean existsByCharacterIdAndConditionIdAndActiveTrue(UUID characterId, UUID conditionId);
}
