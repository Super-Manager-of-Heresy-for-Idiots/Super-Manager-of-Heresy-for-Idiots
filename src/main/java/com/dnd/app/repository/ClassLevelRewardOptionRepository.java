package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClassLevelRewardOptionRepository extends JpaRepository<ClassLevelRewardOption, UUID> {

    Optional<ClassLevelRewardOption> findByGroupIdAndOptionKey(UUID groupId, String optionKey);

    List<ClassLevelRewardOption> findAllByGroupIdOrderBySortOrderAsc(UUID groupId);
}
