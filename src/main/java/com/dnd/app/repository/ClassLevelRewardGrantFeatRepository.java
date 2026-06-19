package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantFeat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantFeatRepository extends JpaRepository<ClassLevelRewardGrantFeat, UUID> {

    List<ClassLevelRewardGrantFeat> findAllByFeatId(UUID featId);
}
