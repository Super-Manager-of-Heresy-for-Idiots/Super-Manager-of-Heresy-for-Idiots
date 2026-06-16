package com.dnd.app.service;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.DictionaryEntry;
import com.dnd.app.domain.EquipmentSlot;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Rarity;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CreatureSizeRepository;
import com.dnd.app.repository.DamageTypeRepository;
import com.dnd.app.repository.DictionaryRepository;
import com.dnd.app.repository.EquipmentSlotRepository;
import com.dnd.app.repository.RarityRepository;
import com.dnd.app.repository.StatTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Resolves item/character content codes (damage type, rarity, equipment slot, size, ability)
 * to their homebrew-friendly dictionary rows. A homebrew package, when supplied, is searched
 * before the system (vanilla) rows so a package can shadow or extend the core set.
 */
@Component
@RequiredArgsConstructor
public class ContentDictionaryResolver {

    private final DamageTypeRepository damageTypeRepository;
    private final RarityRepository rarityRepository;
    private final EquipmentSlotRepository equipmentSlotRepository;
    private final CreatureSizeRepository creatureSizeRepository;
    private final StatTypeRepository statTypeRepository;

    public DamageType resolveDamageType(String code, HomebrewPackage homebrew) {
        return resolveBySlug(code, homebrew,
                damageTypeRepository::findBySlugAndHomebrew_Id,
                damageTypeRepository::findBySlugAndHomebrewIsNull, "damageType");
    }

    public Rarity resolveRarity(String code, HomebrewPackage homebrew) {
        return resolveBySlug(code, homebrew,
                rarityRepository::findBySlugAndHomebrew_Id,
                rarityRepository::findBySlugAndHomebrewIsNull, "rarity");
    }

    public EquipmentSlot resolveEquipmentSlot(String code, HomebrewPackage homebrew) {
        return resolve(equipmentSlotRepository, code, homebrew, "slot");
    }

    /** Equipment slot lookup against the system (vanilla) catalogue only. */
    public EquipmentSlot resolveSystemSlot(String code) {
        return resolve(equipmentSlotRepository, code, null, "slot");
    }

    public void validateSize(String code, UUID homebrewId) {
        validateBySlug(code, homebrewId,
                creatureSizeRepository::existsBySlugAndHomebrew_Id,
                creatureSizeRepository::existsBySlugAndHomebrewIsNull, "size");
    }

    public void validateDamageType(String code, UUID homebrewId) {
        validateBySlug(code, homebrewId,
                damageTypeRepository::existsBySlugAndHomebrew_Id,
                damageTypeRepository::existsBySlugAndHomebrewIsNull, "damageType");
    }

    public void validateAbility(String code, UUID homebrewId) {
        if (code == null) {
            throw new BadRequestException("ability is required");
        }
        String norm = code.toUpperCase();
        if (homebrewId != null && statTypeRepository.existsByCodeAndHomebrewId(norm, homebrewId)) {
            return;
        }
        if (!statTypeRepository.existsByCodeAndHomebrewIsNull(norm)) {
            throw new BadRequestException("Invalid ability: " + code);
        }
    }

    // --- new singular content dictionaries (looked up by slug) ---

    private <T> T resolveBySlug(String code, HomebrewPackage homebrew,
                                BiFunction<String, UUID, Optional<T>> byHomebrew,
                                Function<String, Optional<T>> byVanilla, String label) {
        if (code == null) {
            return null;
        }
        String norm = code.toLowerCase();
        if (homebrew != null) {
            Optional<T> owned = byHomebrew.apply(norm, homebrew.getId());
            if (owned.isPresent()) {
                return owned.get();
            }
        }
        return byVanilla.apply(norm)
                .orElseThrow(() -> new BadRequestException("Invalid " + label + ": " + code));
    }

    private void validateBySlug(String code, UUID homebrewId,
                                BiPredicate<String, UUID> byHomebrew,
                                Predicate<String> byVanilla, String label) {
        if (code == null) {
            throw new BadRequestException(label + " is required");
        }
        String norm = code.toLowerCase();
        if (homebrewId != null && byHomebrew.test(norm, homebrewId)) {
            return;
        }
        if (!byVanilla.test(norm)) {
            throw new BadRequestException("Invalid " + label + ": " + code);
        }
    }

    // --- legacy DictionaryEntry dictionaries (looked up by code) ---

    private <T extends DictionaryEntry> T resolve(DictionaryRepository<T> repo, String code,
                                                  HomebrewPackage homebrew, String label) {
        if (code == null) {
            return null;
        }
        String norm = code.toUpperCase();
        if (homebrew != null) {
            Optional<T> owned = repo.findByCodeAndHomebrewId(norm, homebrew.getId());
            if (owned.isPresent()) {
                return owned.get();
            }
        }
        return repo.findByCodeAndHomebrewIsNull(norm)
                .orElseThrow(() -> new BadRequestException("Invalid " + label + ": " + code));
    }
}
