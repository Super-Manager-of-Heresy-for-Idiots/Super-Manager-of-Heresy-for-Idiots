package com.dnd.app.service;

import com.dnd.app.domain.CreatureSize;
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
        return resolve(damageTypeRepository, code, homebrew, "damageType");
    }

    public Rarity resolveRarity(String code, HomebrewPackage homebrew) {
        return resolve(rarityRepository, code, homebrew, "rarity");
    }

    public EquipmentSlot resolveEquipmentSlot(String code, HomebrewPackage homebrew) {
        return resolve(equipmentSlotRepository, code, homebrew, "slot");
    }

    /** Equipment slot lookup against the system (vanilla) catalogue only. */
    public EquipmentSlot resolveSystemSlot(String code) {
        return resolve(equipmentSlotRepository, code, null, "slot");
    }

    public void validateSize(String code, UUID homebrewId) {
        validateExists(creatureSizeRepository, code, homebrewId, "size");
    }

    public void validateDamageType(String code, UUID homebrewId) {
        validateExists(damageTypeRepository, code, homebrewId, "damageType");
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

    private <T extends DictionaryEntry> void validateExists(DictionaryRepository<T> repo, String code,
                                                            UUID homebrewId, String label) {
        if (code == null) {
            throw new BadRequestException(label + " is required");
        }
        String norm = code.toUpperCase();
        if (homebrewId != null && repo.existsByCodeAndHomebrewId(norm, homebrewId)) {
            return;
        }
        if (!repo.existsByCodeAndHomebrewIsNull(norm)) {
            throw new BadRequestException("Invalid " + label + ": " + code);
        }
    }
}
