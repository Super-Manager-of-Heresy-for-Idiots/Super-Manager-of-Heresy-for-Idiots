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
 * Класс CharacterSpellbookService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class CharacterSpellbookService {

    private final PlayerCharacterRepository characterRepository;
    private final SpellRepository spellRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;

    /**
     * Возвращает список для операции "list" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "learn" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "forget" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     */
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
