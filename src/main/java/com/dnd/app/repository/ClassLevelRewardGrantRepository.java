package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantRepository extends JpaRepository<ClassLevelRewardGrant, UUID> {

    List<ClassLevelRewardGrant> findAllByRewardGroupIdOrderBySortOrderAsc(UUID groupId);

    List<ClassLevelRewardGrant> findAllByRewardOptionIdOrderBySortOrderAsc(UUID optionId);

    List<ClassLevelRewardGrant> findAllByGrantType(String grantType);
}
