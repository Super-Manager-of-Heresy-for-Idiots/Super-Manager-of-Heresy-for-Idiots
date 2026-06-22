package com.dnd.app.repository;

import com.dnd.app.domain.CharacterSpellSlotUsage;
import com.dnd.app.domain.CharacterSpellSlotUsageId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterSpellSlotUsageRepository
        extends JpaRepository<CharacterSpellSlotUsage, CharacterSpellSlotUsageId> {

    List<CharacterSpellSlotUsage> findAllByCharacterId(UUID characterId);

    Optional<CharacterSpellSlotUsage> findByCharacterIdAndSpellLevel(UUID characterId, Integer spellLevel);
}
