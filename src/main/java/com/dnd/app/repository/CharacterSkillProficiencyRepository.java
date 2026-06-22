package com.dnd.app.repository;

import com.dnd.app.domain.CharacterSkillProficiency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterSkillProficiencyRepository extends JpaRepository<CharacterSkillProficiency, UUID> {

    List<CharacterSkillProficiency> findByCharacterId(UUID characterId);

    Optional<CharacterSkillProficiency> findByCharacterIdAndSkillId(UUID characterId, UUID skillId);
}
