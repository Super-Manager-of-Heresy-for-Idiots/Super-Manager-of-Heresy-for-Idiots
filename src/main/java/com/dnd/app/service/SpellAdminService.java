package com.dnd.app.service;

import com.dnd.app.domain.Spell;
import com.dnd.app.dto.content.SpellWarningResponse;
import com.dnd.app.dto.request.SpellResolutionRequest;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Admin-side operations for the spell data-quality review console: listing spells
 * flagged for manual resolution and applying an admin's correction (saving-throw
 * ability / attack-roll flag) while clearing the warning.
 */
@Service
@RequiredArgsConstructor
public class SpellAdminService {

    private static final Set<String> ABILITIES = Set.of(
            "STRENGTH", "DEXTERITY", "CONSTITUTION", "INTELLIGENCE", "WISDOM", "CHARISMA");

    private final SpellRepository spellRepository;

    @Transactional(readOnly = true)
    public List<SpellWarningResponse> listWarnings(String lang) {
        return spellRepository.findWarnings().stream()
                .map(s -> toResponse(s, lang))
                .toList();
    }

    @Transactional
    public SpellWarningResponse resolve(UUID id, SpellResolutionRequest request, String lang) {
        Spell spell = spellRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено: " + id));

        String ability = request.getSaveAbility();
        if (ability != null && !ability.isBlank()) {
            ability = ability.trim().toUpperCase(Locale.ROOT);
            if (!ABILITIES.contains(ability)) {
                throw new IllegalArgumentException("Недопустимая характеристика спасброска: " + request.getSaveAbility());
            }
            spell.setSaveAbility(ability);
        } else {
            spell.setSaveAbility(null);
        }

        if (request.getAttackRoll() != null) {
            spell.setAttackRoll(request.getAttackRoll());
        }

        boolean stillFlagged = Boolean.TRUE.equals(request.getWarning());
        spell.setWarning(stillFlagged);
        if (!stillFlagged) {
            spell.setWarningReason(null);
        }

        spellRepository.save(spell);
        return toResponse(spell, lang);
    }

    private SpellWarningResponse toResponse(Spell s, String lang) {
        String schoolName = s.getSchool() == null ? null
                : Localization.pick(lang, s.getSchool().getNameRu(), s.getSchool().getNameEn(), s.getSchool().getNameRu());
        return SpellWarningResponse.builder()
                .id(s.getId())
                .slug(s.getSlug())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), s.getNameRu()))
                .level(s.getLevel())
                .schoolName(schoolName)
                .saveAbility(s.getSaveAbility())
                .attackRoll(s.getAttackRoll())
                .warning(s.getWarning())
                .warningReason(s.getWarningReason())
                .description(s.getDescription())
                .build();
    }
}
