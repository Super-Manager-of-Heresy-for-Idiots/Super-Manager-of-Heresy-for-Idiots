package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.FeatureUseLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeatureUseLogRepository extends JpaRepository<FeatureUseLog, UUID> {
    List<FeatureUseLog> findTop50ByCharacterIdOrderByCreatedAtDesc(UUID characterId);
}
