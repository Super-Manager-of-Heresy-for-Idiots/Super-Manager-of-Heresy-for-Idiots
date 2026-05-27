package com.dnd.app.repository;

import com.dnd.app.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {
    boolean existsByName(String name);
}
