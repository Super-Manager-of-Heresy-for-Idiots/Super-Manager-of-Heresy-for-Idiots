package com.dnd.app.repository;

import com.dnd.app.domain.CharacterKnownSpell;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CharacterKnownSpellRepository extends JpaRepository<CharacterKnownSpell, UUID> {

    List<CharacterKnownSpell> findByCharacterId(UUID characterId);
}
