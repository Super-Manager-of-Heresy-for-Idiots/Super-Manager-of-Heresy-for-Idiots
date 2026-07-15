package com.dnd.app.service.homebrew;

import com.dnd.app.domain.Spell;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SpellRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Валидатор контента типа SPELL (P2-1). Обеспечивает существование, сводку и владельца homebrew-заклинания
 * при регистрации/резолвинге в пакете. Владелец берётся из автора пакета-родителя (как у прочих типов).
 * Механика заклинания исполняется движком feature-rules (owner_type=SPELL), а не таблицами 056–062.
 */
@Component
@RequiredArgsConstructor
public class SpellContentValidator implements HomebrewContentValidator {

    private final SpellRepository spellRepository;

    @Override
    public String getSupportedType() {
        return "SPELL";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!spellRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Заклинание не найдено: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        Spell spell = require(contentId);
        return ContentSummaryDto.builder()
                .id(spell.getId())
                .name(spell.getNameRu())
                .description(spell.getDescription())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        Spell spell = require(contentId);
        return spell.getHomebrew() != null ? spell.getHomebrew().getAuthor().getId() : null;
    }

    private Spell require(UUID contentId) {
        return spellRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено: " + contentId));
    }
}
