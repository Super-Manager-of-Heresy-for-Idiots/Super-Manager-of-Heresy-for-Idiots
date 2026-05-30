package com.dnd.app.repository;

import com.dnd.app.domain.Condition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ConditionRepository extends JpaRepository<Condition, UUID> {

    boolean existsByName(String name);

    List<Condition> findAllByCreatedById(UUID createdById);

    List<Condition> findAllByTeamId(UUID teamId);

    @Query("SELECT c FROM Condition c WHERE c.team.gameMaster.id = :gmId")
    List<Condition> findAllByGameMasterId(@Param("gmId") UUID gmId);
}
