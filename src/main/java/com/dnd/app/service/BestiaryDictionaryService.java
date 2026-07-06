package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.DictionaryKind;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.DictionaryEntryRequest;
import com.dnd.app.dto.response.DictionaryEntryResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Role-based CRUD for the 10 bestiary reference dictionaries. ADMIN manages system rows
 * (homebrew NULL); GAME_MASTER adds rows scoped to a homebrew package they own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BestiaryDictionaryService {

    private final CreatureTypeRepository creatureTypeRepository;
    private final AlignmentRepository alignmentRepository;
    private final LanguageRepository languageRepository;
    private final SenseTypeRepository senseTypeRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final HabitatRepository habitatRepository;
    private final TreasureTagRepository treasureTagRepository;
    private final BestiaryConditionRepository bestiaryConditionRepository;
    private final MonsterGearItemRepository monsterGearItemRepository;
    private final SourceRepository sourceRepository;
    private final BestiarySizeRepository bestiarySizeRepository;
    private final BestiaryAbilityRepository bestiaryAbilityRepository;
    private final DamageTypeRepository damageTypeRepository;
    private final EquipmentSlotRepository equipmentSlotRepository;
    private final UserRepository userRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;

    private record Handler<T extends DictionaryEntry>(DictionaryRepository<T> repo, Supplier<T> factory) {}

    private final Map<DictionaryKind, Handler<?>> handlers = new EnumMap<>(DictionaryKind.class);

    @PostConstruct
    void init() {
        handlers.put(DictionaryKind.CREATURE_TYPE, new Handler<>(creatureTypeRepository, CreatureType::new));
        handlers.put(DictionaryKind.ALIGNMENT, new Handler<>(alignmentRepository, Alignment::new));
        handlers.put(DictionaryKind.LANGUAGE, new Handler<>(languageRepository, Language::new));
        handlers.put(DictionaryKind.SENSE_TYPE, new Handler<>(senseTypeRepository, SenseType::new));
        handlers.put(DictionaryKind.MOVEMENT_TYPE, new Handler<>(movementTypeRepository, MovementType::new));
        handlers.put(DictionaryKind.HABITAT, new Handler<>(habitatRepository, Habitat::new));
        handlers.put(DictionaryKind.TREASURE_TAG, new Handler<>(treasureTagRepository, TreasureTag::new));
        handlers.put(DictionaryKind.CONDITION, new Handler<>(bestiaryConditionRepository, BestiaryCondition::new));
        handlers.put(DictionaryKind.GEAR_ITEM, new Handler<>(monsterGearItemRepository, MonsterGearItem::new));
        handlers.put(DictionaryKind.SOURCE, new Handler<>(sourceRepository, Source::new));
        handlers.put(DictionaryKind.SIZE, new Handler<>(bestiarySizeRepository, BestiarySize::new));
        handlers.put(DictionaryKind.ABILITY, new Handler<>(bestiaryAbilityRepository, BestiaryAbility::new));
        handlers.put(DictionaryKind.DAMAGE_TYPE, new Handler<>(damageTypeRepository, DamageType::new));
        handlers.put(DictionaryKind.EQUIPMENT_SLOT, new Handler<>(equipmentSlotRepository, EquipmentSlot::new));
    }

    // --- Reads ---

    @Transactional(readOnly = true)
    public List<DictionaryEntryResponse> listSystem(DictionaryKind kind) {
        return handler(kind).repo().findAllByHomebrewIsNull().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<DictionaryEntryResponse> listForHomebrew(DictionaryKind kind, UUID packageId) {
        return handler(kind).repo().findAllByHomebrewId(packageId).stream().map(this::toResponse).toList();
    }

    // --- ADMIN system CRUD ---

    @Transactional
    public DictionaryEntryResponse createSystem(DictionaryKind kind, DictionaryEntryRequest request, String username) {
        requireRole(username, Role.ADMIN);
        Handler<?> h = handler(kind);
        if (h.repo().existsByCodeAndHomebrewIsNull(request.getCode())) {
            throw new DuplicateResourceException("System " + kind.getSlug() + " with code '" + request.getCode() + "' already exists");
        }
        DictionaryEntry entry = h.factory().get();
        apply(entry, request, null);
        DictionaryEntry saved = save(h, entry);
        log.info("System {} created: id={}, code='{}', by={}", kind.getSlug(), saved.getId(), saved.getCode(), username);
        return toResponse(saved);
    }

    @Transactional
    public DictionaryEntryResponse updateSystem(DictionaryKind kind, UUID id, DictionaryEntryRequest request, String username) {
        requireRole(username, Role.ADMIN);
        Handler<?> h = handler(kind);
        DictionaryEntry entry = findEntry(h, id);
        if (entry.getHomebrew() != null) {
            throw new BadRequestException("Only system dictionary entries can be updated through the admin endpoint");
        }
        if (!entry.getCode().equals(request.getCode()) && h.repo().existsByCodeAndHomebrewIsNull(request.getCode())) {
            throw new DuplicateResourceException("System " + kind.getSlug() + " with code '" + request.getCode() + "' already exists");
        }
        apply(entry, request, null);
        return toResponse(save(h, entry));
    }

    @Transactional
    public void deleteSystem(DictionaryKind kind, UUID id, String username) {
        requireRole(username, Role.ADMIN);
        Handler<?> h = handler(kind);
        DictionaryEntry entry = findEntry(h, id);
        if (entry.getHomebrew() != null) {
            throw new BadRequestException("Only system dictionary entries can be deleted through the admin endpoint");
        }
        h.repo().deleteById(id);
        log.info("System {} deleted: id={}, by={}", kind.getSlug(), id, username);
    }

    // --- GM homebrew CRUD ---

    @Transactional
    public DictionaryEntryResponse createHomebrew(DictionaryKind kind, UUID packageId, DictionaryEntryRequest request, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        Handler<?> h = handler(kind);
        if (h.repo().existsByCodeAndHomebrewId(request.getCode(), pkg.getId())) {
            throw new DuplicateResourceException("Homebrew " + kind.getSlug() + " with code '" + request.getCode() + "' already exists in this package");
        }
        DictionaryEntry entry = h.factory().get();
        apply(entry, request, pkg);
        DictionaryEntry saved = save(h, entry);
        log.info("Homebrew {} created: id={}, packageId={}, by={}", kind.getSlug(), saved.getId(), packageId, username);
        return toResponse(saved);
    }

    @Transactional
    public DictionaryEntryResponse updateHomebrew(DictionaryKind kind, UUID packageId, UUID id, DictionaryEntryRequest request, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        Handler<?> h = handler(kind);
        DictionaryEntry entry = findEntry(h, id);
        requireSamePackage(entry, pkg);
        if (!entry.getCode().equals(request.getCode()) && h.repo().existsByCodeAndHomebrewId(request.getCode(), pkg.getId())) {
            throw new DuplicateResourceException("Homebrew " + kind.getSlug() + " with code '" + request.getCode() + "' already exists in this package");
        }
        apply(entry, request, pkg);
        return toResponse(save(h, entry));
    }

    @Transactional
    public void deleteHomebrew(DictionaryKind kind, UUID packageId, UUID id, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        Handler<?> h = handler(kind);
        DictionaryEntry entry = findEntry(h, id);
        requireSamePackage(entry, pkg);
        h.repo().deleteById(id);
        log.info("Homebrew {} deleted: id={}, packageId={}, by={}", kind.getSlug(), id, packageId, username);
    }

    // --- helpers ---

    private void apply(DictionaryEntry entry, DictionaryEntryRequest request, HomebrewPackage pkg) {
        entry.setCode(request.getCode());
        entry.setNameRusloc(request.getNameRusloc());
        entry.setNameEngloc(request.getNameEngloc());
        entry.setHomebrew(pkg);
        entry.setIsUnique(request.getIsUnique() != null ? request.getIsUnique() : false);
        if (entry instanceof Source source) {
            source.setBookCode(request.getBookCode());
        }
    }

    private DictionaryEntryResponse toResponse(DictionaryEntry entry) {
        return DictionaryEntryResponse.builder()
                .id(entry.getId())
                .code(entry.getCode())
                .nameRusloc(entry.getNameRusloc())
                .nameEngloc(entry.getNameEngloc())
                .bookCode(entry instanceof Source source ? source.getBookCode() : null)
                .homebrewId(entry.getHomebrew() != null ? entry.getHomebrew().getId() : null)
                .isUnique(entry.getIsUnique())
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private Handler<?> handler(DictionaryKind kind) {
        Handler<?> h = handlers.get(kind);
        if (h == null) {
            throw new ResourceNotFoundException("Unknown dictionary: " + kind);
        }
        return h;
    }

    private DictionaryEntry findEntry(Handler<?> h, UUID id) {
        return h.repo().findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictionary entry not found"));
    }

    @SuppressWarnings("unchecked")
    private DictionaryEntry save(Handler<?> h, DictionaryEntry entry) {
        return ((DictionaryRepository<DictionaryEntry>) (DictionaryRepository<?>) h.repo()).save(entry);
    }

    private void requireSamePackage(DictionaryEntry entry, HomebrewPackage pkg) {
        if (entry.getHomebrew() == null || !entry.getHomebrew().getId().equals(pkg.getId())) {
            throw new AccessDeniedException("Dictionary entry does not belong to this homebrew package");
        }
    }

    private HomebrewPackage getEditablePackage(UUID packageId, User gm) {
        HomebrewPackage pkg = homebrewPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package not found"));
        if (gm.getRole() != Role.ADMIN && !pkg.getAuthor().getId().equals(gm.getId())) {
            throw new AccessDeniedException("Cannot manage dictionaries in another user's homebrew package");
        }
        if (pkg.isDeleted() || pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Homebrew dictionary content can be changed only in a DRAFT package");
        }
        return pkg;
    }

    private User requireGameMaster(String username) {
        User user = getUser(username);
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only game masters can manage homebrew dictionaries");
        }
        return user;
    }

    private User requireRole(String username, Role role) {
        User user = getUser(username);
        if (user.getRole() != role) {
            throw new AccessDeniedException("Required role: " + role);
        }
        return user;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
