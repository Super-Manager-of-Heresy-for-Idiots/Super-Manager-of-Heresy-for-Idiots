package com.dnd.app.repository;

import com.dnd.app.domain.Condition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConditionRepository extends JpaRepository<Condition, UUID> {

    boolean existsByName(String name);

    List<Condition> findAllByCreatedById(UUID createdById);
}
