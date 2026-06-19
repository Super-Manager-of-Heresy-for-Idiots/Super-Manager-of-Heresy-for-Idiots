package com.dnd.app.repository;

import com.dnd.app.domain.content.ContentSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ContentSkillRepository extends JpaRepository<ContentSkill, UUID> {

    Optional<ContentSkill> findBySlugAndHomebrewIsNull(String slug);

    List<ContentSkill> findAllByHomebrewIsNull();

    List<ContentSkill> findAllByHomebrewIdIn(Set<UUID> homebrewIds);

    List<ContentSkill> findAllByIdIn(Set<UUID> ids);
}
