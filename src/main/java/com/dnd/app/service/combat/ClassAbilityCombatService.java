package com.dnd.app.service.combat;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.response.CharacterAttackResponse;
import com.dnd.app.dto.response.ClassAbilityResponse;
import com.dnd.app.repository.ClassFeatureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс ClassAbilityCombatService описывает сервис боевой логики, который рассчитывает и применяет правила боя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class ClassAbilityCombatService {

    private static final String STR = "str";
    private static final String DEX = "dex";

    private final ClassFeatureRepository classFeatureRepository;

    /**
     * Возвращает список для операции "list abilities" в рамках бизнес-логики боя.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public List<ClassAbilityResponse> listAbilities(PlayerCharacter character) {
        List<ClassAbilityResponse> result = new ArrayList<>();
        if (character.getClassLevels() == null) {
            return result;
        }
        for (CharacterClassLevel cl : character.getClassLevels()) {
            ContentCharacterClass cls = cl.getCharacterClass();
            if (cls == null) {
                continue;
            }
            int classLevel = cl.getClassLevel() != null ? cl.getClassLevel() : 1;
            String className = cls.getNameRu() != null ? cls.getNameRu() : cls.getNameEn();
            for (ClassFeature f : classFeatureRepository.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(cls.getId())) {
                int featureLevel = f.getLevel() != null ? f.getLevel() : 1;
                if (featureLevel > classLevel) {
                    continue; // not yet unlocked
                }
                String damage = f.getDamageDice() != null && !f.getDamageDice().isBlank()
                        ? f.getDamageDice()
                        : AttackResolver.extractDamageExpression(f.getDescription());
                result.add(ClassAbilityResponse.builder()
                        .id(f.getId())
                        .name(f.getTitle())
                        .level(featureLevel)
                        .className(className)
                        .description(f.getDescription())
                        .activationType(f.getActivationType())
                        .attackRoll(Boolean.TRUE.equals(f.getAttackRoll()))
                        .saveAbility(f.getSaveAbility())
                        .usableAsAttack(damage != null)
                        .damage(damage)
                        .damageType(f.getDamageType())
                        .healingDice(f.getHealingDice())
                        .healingFlat(f.getHealingFlat())
                        .warning(Boolean.TRUE.equals(f.getWarning()))
                        .build());
            }
        }
        return result;
    }

    /**
     * Выполняет операции "class attacks" в рамках бизнес-логики боя.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public List<CharacterAttackResponse> classAttacks(PlayerCharacter character) {
        int profBonus = proficiencyBonus(character.getTotalLevel() != null ? character.getTotalLevel() : 1);
        int abilityMod = Math.max(abilityModifier(character, STR), abilityModifier(character, DEX));
        int attackBonus = abilityMod + profBonus;

        List<CharacterAttackResponse> attacks = new ArrayList<>();
        for (ClassAbilityResponse ability : listAbilities(character)) {
            if (!ability.isUsableAsAttack()) {
                continue;
            }
            attacks.add(CharacterAttackResponse.builder()
                    .name(ability.getName())
                    .attackBonus(signed(attackBonus))
                    .damage(ability.getDamage())
                    .damageType(ability.getDamageType())
                    .category("CLASS")
                    .source("CLASS")
                    .build());
        }
        return attacks;
    }

    // --- helpers ---

    private int abilityModifier(PlayerCharacter character, String slug) {
        if (character.getStats() == null) {
            return 0;
        }
        int score = character.getStats().stream()
                .filter(s -> s.getStatType() != null && slug.equalsIgnoreCase(s.getStatType().getSlug()))
                .findFirst()
                .map(CharacterStat::getValue)
                .orElse(10);
        return CombatCalculator.abilityModifier(score);
    }

    /** D&D 5e proficiency bonus from total level: 2 at 1–4, 3 at 5–8, etc. */
    private int proficiencyBonus(int totalLevel) {
        return 2 + (Math.max(1, totalLevel) - 1) / 4;
    }

    private static String signed(int v) {
        return (v >= 0 ? "+" : "") + v;
    }
}
