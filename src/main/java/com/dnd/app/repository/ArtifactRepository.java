package com.dnd.app.repository;

import com.dnd.app.domain.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<Artifact, UUID> {

    List<Artifact> findAllByCreatedById(UUID createdById);
}
