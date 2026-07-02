package com.dnd.app.service;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.content.SpellDamage;
import com.dnd.app.domain.content.SpellHealing;
import com.dnd.app.dto.content.SpellDetailResponse;
import com.dnd.app.dto.content.SpellWarningResponse;
import com.dnd.app.dto.request.SpellEditRequest;
import com.dnd.app.dto.request.SpellResolutionRequest;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.SpellMapper;
import com.dnd.app.repository.DamageTypeRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
    private final DamageTypeRepository damageTypeRepository;
    private final SpellMapper spellMapper;

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

    /**
     * Full admin edit of a spell's combat resolution: save ability, attack-roll flag,
     * ability-check ability + skill, warning flag, and the base damage / healing entries.
     * The damage and healing lists, when present, replace the spell's current entries
     * wholesale; a null list leaves that collection untouched.
     */
    @Transactional
    public SpellDetailResponse update(UUID id, SpellEditRequest request, String lang) {
        Spell spell = spellRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено: " + id));

        spell.setSaveAbility(normalizeAbility(request.getSaveAbility(), "спасброска"));
        if (request.getAttackRoll() != null) {
            spell.setAttackRoll(request.getAttackRoll());
        }

        spell.setCheckAbility(normalizeAbility(request.getCheckAbility(), "проверки"));
        String skill = request.getCheckSkill();
        spell.setCheckSkill(skill != null && !skill.isBlank() ? skill.trim() : null);

        if (request.getWarning() != null) {
            boolean stillFlagged = request.getWarning();
            spell.setWarning(stillFlagged);
            if (!stillFlagged) {
                spell.setWarningReason(null);
            }
        }

        if (request.getDamages() != null) {
            List<SpellDamage> damages = new ArrayList<>();
            for (SpellEditRequest.DamageRow row : request.getDamages()) {
                damages.add(SpellDamage.builder()
                        .dice(normDice(row.getDice()))
                        .damageType(resolveDamageType(row.getDamageTypeSlug()))
                        .raw(blankToNull(row.getRaw()))
                        .build());
            }
            spell.getDamages().clear();
            spell.getDamages().addAll(damages);
        }

        if (request.getHealings() != null) {
            List<SpellHealing> healings = new ArrayList<>();
            for (SpellEditRequest.HealingRow row : request.getHealings()) {
                healings.add(SpellHealing.builder()
                        .dice(normDice(row.getDice()))
                        .flat(row.getFlat())
                        .raw(blankToNull(row.getRaw()))
                        .build());
            }
            spell.getHealings().clear();
            spell.getHealings().addAll(healings);
        }

        spellRepository.save(spell);
        return spellMapper.toDetail(spell, lang);
    }

    /** Validates an ability code (STRENGTH..CHARISMA); returns the upper-cased code, or null when blank. */
    private String normalizeAbility(String value, String contextRu) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String ability = value.trim().toUpperCase(Locale.ROOT);
        if (!ABILITIES.contains(ability)) {
            throw new IllegalArgumentException("Недопустимая характеристика " + contextRu + ": " + value);
        }
        return ability;
    }

    /** Resolves a core damage-type by slug; null/blank slug means a typeless damage entry. */
    private DamageType resolveDamageType(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return damageTypeRepository.findBySlugAndHomebrewIsNull(slug.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Тип урона не найден: " + slug));
    }

    /** Canonicalises Cyrillic dice notation ("1к6") to the neutral "1d6" form; blank -> null. */
    private static String normDice(String dice) {
        if (dice == null || dice.isBlank()) {
            return null;
        }
        return dice.trim().replace('к', 'd').replace('К', 'd');
    }

    private static String blankToNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
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
