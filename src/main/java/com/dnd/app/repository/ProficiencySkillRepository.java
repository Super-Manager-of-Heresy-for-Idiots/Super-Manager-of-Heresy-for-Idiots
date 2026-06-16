package com.dnd.app.repository;

import com.dnd.app.domain.ProficiencySkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ProficiencySkillRepository extends JpaRepository<ProficiencySkill, UUID> {

    Optional<ProficiencySkill> findByName(String name);

    List<ProficiencySkill> findByNameIn(List<String> names);

    List<ProficiencySkill> findByIdIn(Set<UUID> ids);
}
