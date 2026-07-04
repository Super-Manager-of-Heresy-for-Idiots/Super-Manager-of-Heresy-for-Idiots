package com.dnd.app.service;

import com.dnd.app.domain.CharacterKnownSpell;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.Spell;
import com.dnd.app.dto.response.CharacterKnownSpellResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Manages a character's recorded spells (the "spellbook"): list / learn / forget. This is the
 * player-facing spell management the folio was missing (a Wizard could not record newly learned spells;
 * spells were only ever set at level-up). It mutates {@code character_known_spells} directly and is
 * independent of the feature-rules runtime flags. Access is enforced by the caller (owner/GM/ADMIN).
 *
 * <p>The backend stays permissive (any existing spell not already known can be recorded); the frontend
 * picker is responsible for offering only class/level-appropriate spells.</p>
 */
@Service
@RequiredArgsConstructor
public class CharacterSpellbookService {

    private final PlayerCharacterRepository characterRepository;
    private final SpellRepository spellRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;

    @Transactional(readOnly = true)
    public List<CharacterKnownSpellResponse> list(UUID characterId) {
        return knownSpellRepository.findByCharacterId(characterId).stream()
                .map(ks -> toResponse(ks.getSpell()))
                .sorted(Comparator.comparing(CharacterKnownSpellResponse::getLevel,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CharacterKnownSpellResponse::getName,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional
    public CharacterKnownSpellResponse learn(UUID characterId, UUID spellId) {
        if (knownSpellRepository.existsByCharacterIdAndSpellId(characterId, spellId)) {
            throw new BadRequestException("Заклинание уже записано в книгу");
        }
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        Spell spell = spellRepository.findById(spellId)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено"));
        knownSpellRepository.save(CharacterKnownSpell.builder()
                .character(character)
                .spell(spell)
                .build());
        return toResponse(spell);
    }

    @Transactional
    public void forget(UUID characterId, UUID spellId) {
        knownSpellRepository.findByCharacterIdAndSpellId(characterId, spellId)
                .ifPresent(knownSpellRepository::delete);
    }

    private CharacterKnownSpellResponse toResponse(Spell spell) {
        return CharacterKnownSpellResponse.builder()
                .spellId(spell.getId())
                .name(spell.getNameRu())
                .level(spell.getLevel())
                .school(spell.getSchool() == null ? null : spell.getSchool().getNameRu())
                .build();
    }
}
