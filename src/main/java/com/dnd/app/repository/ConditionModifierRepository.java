package com.dnd.app.repository;

import com.dnd.app.domain.ConditionModifier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConditionModifierRepository extends JpaRepository<ConditionModifier, UUID> {

    boolean existsByConditionIdAndStatTypeId(UUID conditionId, UUID statTypeId);
}
