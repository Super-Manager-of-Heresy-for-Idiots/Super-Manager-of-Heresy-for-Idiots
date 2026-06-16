package com.dnd.app.repository;

import com.dnd.app.domain.content.ClassLevelRewardGrantSkillProficiency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ClassLevelRewardGrantSkillProficiencyRepository
        extends JpaRepository<ClassLevelRewardGrantSkillProficiency, UUID> {

    List<ClassLevelRewardGrantSkillProficiency> findAllByFixedSkillId(UUID skillId);
}
