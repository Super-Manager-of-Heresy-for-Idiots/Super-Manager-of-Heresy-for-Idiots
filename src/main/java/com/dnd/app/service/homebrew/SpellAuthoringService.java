package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewContentItem;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.SpellSchool;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.dto.request.HomebrewSpellRequest;
import com.dnd.app.dto.response.HomebrewSpellResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.SpellSchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Авторинг homebrew-заклинания (P2-1, Phase A: идентичность). Пакет-scoped CRUD по образцу авторинга видов/предметов:
 * homebrew_id = packageId, created_by/updated_by = автор, уникальный slug в пакете, резолв школы по slug и доступных
 * классов; авто-регистрация {@code HomebrewContentItem(SPELL)}. Механика (урон/спасбросок/эффекты) — отдельно через
 * движок feature-rules (owner_type=SPELL), таблицы 056–062 для homebrew НЕ заполняются (мораторий).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellAuthoringService {

    private final SpellRepository spellRepository;
    private final SpellSchoolRepository spellSchoolRepository;
    private final ContentCharacterClassRepository classRepository;
    private final HomebrewAccessService homebrewAccessService;
    private final HomebrewContentItemRepository contentItemRepository;
    private final SpellMechanicsService spellMechanicsService;

    /**
     * Создаёт заклинание в пакете.
     * @param packageId пакет-владелец (редактируемый)
     * @param request тело заклинания
     * @param username автор
     * @return созданное заклинание
     */
    @Transactional
    public HomebrewSpellResponse create(UUID packageId, HomebrewSpellRequest request, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        Spell spell = new Spell();
        spell.setHomebrew(pkg);
        spell.setCreatedBy(pkg.getAuthor());
        spell.setSlug(uniqueSlug(slugify(request.getName()), pkg.getId()));
        apply(spell, request, pkg);
        Spell saved = spellRepository.save(spell);
        registerContentItem(pkg, saved.getId());
        spellMechanicsService.sync(saved, pkg, request, username);
        log.info("Homebrew spell created: id={}, packageId={}, by={}", saved.getId(), packageId, username);
        return toResponse(saved);
    }

    /**
     * Обновляет заклинание пакета.
     */
    @Transactional
    public HomebrewSpellResponse update(UUID packageId, UUID spellId, HomebrewSpellRequest request, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        Spell spell = requirePackageSpell(packageId, spellId);
        apply(spell, request, pkg);
        Spell saved = spellRepository.save(spell);
        spellMechanicsService.sync(saved, pkg, request, username);
        log.info("Homebrew spell updated: id={}, packageId={}, by={}", spellId, packageId, username);
        return toResponse(saved);
    }

    /**
     * Читает заклинание пакета (для редактора).
     */
    @Transactional(readOnly = true)
    public HomebrewSpellResponse get(UUID packageId, UUID spellId, String username) {
        homebrewAccessService.enforceReadable(packageId, username);
        return toResponse(requirePackageSpell(packageId, spellId));
    }

    /**
     * Удаляет заклинание пакета. 409 при использовании (известно персонажам / в книге заклинаний).
     */
    @Transactional
    public void delete(UUID packageId, UUID spellId, String username) {
        editablePackage(packageId, username);
        Spell spell = requirePackageSpell(packageId, spellId);
        contentItemRepository.findAllByHomebrewPackageId(packageId).stream()
                .filter(ci -> ci.getContentType() == ContentType.SPELL && ci.getContentId().equals(spellId))
                .forEach(contentItemRepository::delete);
        try {
            spellRepository.delete(spell);
            spellRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Заклинание используется и не может быть удалено");
        }
        log.info("Homebrew spell deleted: id={}, packageId={}, by={}", spellId, packageId, username);
    }

    // ================= helpers =================

    private void apply(Spell spell, HomebrewSpellRequest request, HomebrewPackage pkg) {
        spell.setUpdatedBy(pkg.getAuthor());
        spell.setNameRu(request.getName());
        spell.setNameEn(request.getNameEn());
        spell.setLevel(request.getLevel());
        spell.setSchool(resolveSchool(request.getSchool()));
        spell.setCastingTimeRaw(request.getCastingTimeRaw());
        spell.setRitual(Boolean.TRUE.equals(request.getRitual()));
        spell.setRangeType(request.getRangeText());
        spell.setDurationRaw(request.getDurationText());
        spell.setConcentration(Boolean.TRUE.equals(request.getConcentration()));
        spell.setDescription(request.getDescription());
        spell.setHigherLevels(request.getHigherLevels());
        spell.setClasses(resolveClasses(request.getAvailableToClassIds()));
    }

    private SpellSchool resolveSchool(String slug) {
        if (slug == null || slug.isBlank()) {
            throw new BadRequestException("Школа магии обязательна");
        }
        return spellSchoolRepository.findBySlug(slug.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new BadRequestException("Неизвестная школа магии: " + slug));
    }

    private java.util.Set<ContentCharacterClass> resolveClasses(List<UUID> classIds) {
        if (classIds == null || classIds.isEmpty()) {
            return new HashSet<>();
        }
        List<ContentCharacterClass> found = classRepository.findAllById(classIds);
        if (found.size() != new HashSet<>(classIds).size()) {
            throw new BadRequestException("Один или несколько классов не найдены");
        }
        return new HashSet<>(found);
    }

    private HomebrewPackage editablePackage(UUID packageId, String username) {
        HomebrewPackage pkg = homebrewAccessService.enforceOwner(packageId, username);
        if (pkg.isDeleted() || !pkg.getStatus().isEditable()) {
            throw new BadRequestException("Заклинания можно менять только в DRAFT/PUBLISHED-пакете");
        }
        return pkg;
    }

    private Spell requirePackageSpell(UUID packageId, UUID spellId) {
        return spellRepository.findByIdAndHomebrew_Id(spellId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено в этом пакете"));
    }

    private void registerContentItem(HomebrewPackage pkg, UUID spellId) {
        if (!contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                pkg.getId(), ContentType.SPELL, spellId)) {
            contentItemRepository.save(HomebrewContentItem.builder()
                    .homebrewPackage(pkg)
                    .contentType(ContentType.SPELL)
                    .contentId(spellId)
                    .build());
        }
    }

    private HomebrewSpellResponse toResponse(Spell spell) {
        HomebrewSpellResponse resp = HomebrewSpellResponse.builder()
                .id(spell.getId())
                .name(spell.getNameRu())
                .nameEn(spell.getNameEn())
                .level(spell.getLevel())
                .school(spell.getSchool() != null ? spell.getSchool().getSlug() : null)
                .castingTimeRaw(spell.getCastingTimeRaw())
                .ritual(spell.getRitual())
                .rangeText(spell.getRangeType())
                .durationText(spell.getDurationRaw())
                .concentration(spell.getConcentration())
                .description(spell.getDescription())
                .higherLevels(spell.getHigherLevels())
                .availableToClassIds(spell.getClasses().stream().map(ContentCharacterClass::getId).collect(Collectors.toList()))
                .source(spell.getHomebrew() != null ? "HOMEBREW" : "GLOBAL")
                .homebrewPackageId(spell.getHomebrew() != null ? spell.getHomebrew().getId() : null)
                .homebrewPackageTitle(spell.getHomebrew() != null ? spell.getHomebrew().getTitle() : null)
                .build();
        spellMechanicsService.read(spell, resp);
        return resp;
    }

    private String uniqueSlug(String base, UUID packageId) {
        String root = (base == null || base.isBlank())
                ? "spell-" + UUID.randomUUID().toString().substring(0, 8) : base;
        String candidate = root;
        int n = 2;
        while (spellRepository.existsBySlugAndHomebrew_Id(candidate, packageId)) {
            candidate = root + "-" + n++;
        }
        return candidate;
    }

    private static String slugify(String s) {
        if (s == null) {
            return "";
        }
        String slug = s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9а-я]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isBlank() ? "spell" : slug;
    }
}
