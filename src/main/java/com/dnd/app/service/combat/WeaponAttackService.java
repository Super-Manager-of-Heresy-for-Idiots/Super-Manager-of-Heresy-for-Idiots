package com.dnd.app.service.combat;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.WeaponItemProperty;
import com.dnd.app.domain.content.WeaponStat;
import com.dnd.app.dto.response.CharacterAttackResponse;
import com.dnd.app.mapper.ItemInstanceMapper;
import com.dnd.app.repository.ItemInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Класс WeaponAttackService описывает сервис боевой логики, который рассчитывает и применяет правила боя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class WeaponAttackService {

    private static final String STR = "str";
    private static final String DEX = "dex";
    private static final String CAT_MELEE = "MELEE";
    private static final String CAT_RANGED = "RANGED";
    private static final String CAT_THROWN = "THROWN";

    private final ItemInstanceRepository itemInstanceRepository;

    /**
     * Выполняет операции "compute attacks" в рамках бизнес-логики боя.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public List<CharacterAttackResponse> computeAttacks(PlayerCharacter character) {
        List<CharacterAttackResponse> attacks = new ArrayList<>();
        List<ItemInstance> equipped = itemInstanceRepository.findByOwnerCharacterIdAndSlotIsNotNull(character.getId());

        int strMod = CombatCalculator.abilityModifier(abilityScore(character, STR));
        int dexMod = CombatCalculator.abilityModifier(abilityScore(character, DEX));
        int profBonus = proficiencyBonus(character.getTotalLevel() != null ? character.getTotalLevel() : 1);

        for (ItemInstance instance : equipped) {
            WeaponStat weaponStat = instance.getWeaponStat();
            if (weaponStat == null) {
                continue; // not an equipment-model weapon
            }
            EquipmentItem weapon = instance.getEquipmentItem();
            Set<String> props = properties(weapon);

            boolean finesse = props.contains("finesse");
            boolean ranged = props.contains("ammunition");
            boolean thrown = props.contains("thrown");

            int abilityMod;
            if (finesse) {
                abilityMod = Math.max(strMod, dexMod);
            } else if (ranged) {
                abilityMod = dexMod;
            } else {
                abilityMod = strMod;
            }

            boolean proficient = isProficient(character, weapon);
            int attackBonus = abilityMod + (proficient ? profBonus : 0);
            int flat = weaponStat.getFlatDamage() != null ? weaponStat.getFlatDamage() : 0;
            int damageBonus = abilityMod + flat;

            String name = instance.getDisplayName();
            String damageType = damageType(weaponStat);
            String baseCategory = ranged ? CAT_RANGED : CAT_MELEE;

            // Primary strike (melee, or a shot for ammunition weapons).
            attacks.add(attack(name, attackBonus,
                    ItemInstanceMapper.formatDice(weaponStat.getDamageDiceFormula()), damageBonus, damageType,
                    baseCategory, ranged ? "дальний" : "5 фт."));

            // Throw action for thrown weapons.
            if (thrown) {
                attacks.add(attack(name + " (метание)", attackBonus,
                        ItemInstanceMapper.formatDice(weaponStat.getDamageDiceFormula()), damageBonus, damageType,
                        CAT_THROWN, "20/60 фт."));
            }

            // Two-handed variant for versatile weapons (uses the versatile dice when authored).
            String versatileDice = versatileDice(weapon);
            if (versatileDice != null) {
                attacks.add(attack(name + " (двумя руками)", attackBonus, versatileDice, damageBonus, damageType,
                        CAT_MELEE, "5 фт."));
            }
        }
        return attacks;
    }

    // --- helpers ---

    private CharacterAttackResponse attack(String name, int attackBonus, String dice, int damageBonus,
                                           String damageType, String category, String range) {
        return CharacterAttackResponse.builder()
                .name(name)
                .attackBonus(signed(attackBonus))
                .damage(appendBonus(dice, damageBonus))
                .damageType(damageType)
                .category(category)
                .source("WEAPON")
                .range(range)
                .build();
    }

    private int abilityScore(PlayerCharacter character, String slug) {
        if (character.getStats() == null) {
            return 10;
        }
        return character.getStats().stream()
                .filter(s -> s.getStatType() != null && slug.equalsIgnoreCase(s.getStatType().getSlug()))
                .findFirst()
                .map(CharacterStat::getValue)
                .orElse(10);
    }

    /** D&D 5e proficiency bonus from total level: 2 at 1–4, 3 at 5–8, etc. */
    private int proficiencyBonus(int totalLevel) {
        return 2 + (Math.max(1, totalLevel) - 1) / 4;
    }

    private Set<String> properties(EquipmentItem weapon) {
        Set<String> slugs = new HashSet<>();
        if (weapon == null || weapon.getWeaponProperties() == null) {
            return slugs;
        }
        for (WeaponItemProperty wp : weapon.getWeaponProperties()) {
            if (wp.getWeaponProperty() != null && wp.getWeaponProperty().getSlug() != null) {
                slugs.add(wp.getWeaponProperty().getSlug().toLowerCase(Locale.ROOT));
            }
        }
        return slugs;
    }

    private String versatileDice(EquipmentItem weapon) {
        if (weapon == null || weapon.getWeaponProperties() == null) {
            return null;
        }
        return weapon.getWeaponProperties().stream()
                .filter(wp -> wp.getVersatileDiceFormula() != null)
                .findFirst()
                .map(wp -> ItemInstanceMapper.formatDice(wp.getVersatileDiceFormula()))
                .orElse(null);
    }

    /**
     * Whether the character's class grants proficiency with this weapon. Matches the weapon's
     * category (simple / martial) or its name against each class's free-text weapon proficiency
     * description (Russian or English).
     */
    private boolean isProficient(PlayerCharacter character, EquipmentItem weapon) {
        if (character.getClassLevels() == null || weapon == null) {
            return false;
        }
        String categorySlug = weapon.getCategory() != null && weapon.getCategory().getSlug() != null
                ? weapon.getCategory().getSlug().toLowerCase(Locale.ROOT) : "";
        boolean simple = categorySlug.startsWith("simple");
        boolean martial = categorySlug.startsWith("martial");
        String nameRu = weapon.getNameRu() != null ? weapon.getNameRu().toLowerCase(Locale.ROOT) : null;
        String nameEn = weapon.getNameEn() != null ? weapon.getNameEn().toLowerCase(Locale.ROOT) : null;

        for (CharacterClassLevel cl : character.getClassLevels()) {
            if (cl.getCharacterClass() == null) {
                continue;
            }
            String text = cl.getCharacterClass().getWeaponProficiencyText();
            if (text == null || text.isBlank()) {
                continue;
            }
            String t = text.toLowerCase(Locale.ROOT);
            if (simple && (t.contains("простое") || t.contains("simple"))) {
                return true;
            }
            if (martial && (t.contains("воинское") || t.contains("martial"))) {
                return true;
            }
            if (nameRu != null && !nameRu.isBlank() && t.contains(nameRu)) {
                return true;
            }
            if (nameEn != null && !nameEn.isBlank() && t.contains(nameEn)) {
                return true;
            }
        }
        return false;
    }

    private String damageType(WeaponStat weaponStat) {
        if (weaponStat.getDamageType() == null) {
            return null;
        }
        String ru = weaponStat.getDamageType().getNameRu();
        return ru != null && !ru.isBlank() ? ru : weaponStat.getDamageType().getSlug();
    }

    private static String signed(int v) {
        return (v >= 0 ? "+" : "") + v;
    }

    /** Appends a flat damage bonus to a dice string, e.g. ("1d8", 3) -> "1d8+3". */
    private static String appendBonus(String dice, int bonus) {
        if (dice == null || dice.isBlank()) {
            return bonus != 0 ? String.valueOf(bonus) : "0";
        }
        if (bonus == 0) {
            return dice;
        }
        return dice + (bonus > 0 ? "+" : "") + bonus;
    }
}
