package com.dnd.app.repository;

import com.dnd.app.domain.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    List<Artifact> findAllByCreatedById(UUID createdById);

    List<Artifact> findAllByTeamId(UUID teamId);

    @Query("SELECT a FROM Artifact a WHERE a.team.gameMaster.id = :gmId")
    List<Artifact> findAllByGameMasterId(@Param("gmId") UUID gmId);
}
