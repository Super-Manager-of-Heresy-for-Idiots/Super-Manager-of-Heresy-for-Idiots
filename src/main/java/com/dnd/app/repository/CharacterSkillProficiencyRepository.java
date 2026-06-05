package com.dnd.app.repository;

import com.dnd.app.domain.CharacterSkillProficiency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterSkillProficiencyRepository extends JpaRepository<CharacterSkillProficiency, UUID> {

    List<CharacterSkillProficiency> findByCharacterId(UUID characterId);
}
