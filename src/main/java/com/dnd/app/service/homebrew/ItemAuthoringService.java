package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewContentItem;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Rarity;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.dto.request.HomebrewItemRequest;
import com.dnd.app.dto.response.HomebrewItemResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.MagicItemRepository;
import com.dnd.app.service.ContentDictionaryResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

/**
 * Авторинг единого homebrew-предмета (P1.5 / IT-2). Снаружи «Предмет» — одна сущность; сервер маршрутизирует по
 * {@code kind}. Реализован MAGIC (magic_item); EQUIPMENT — следующий шаг, TEMPLATE для новых определений запрещён.
 * homebrew_id = packageId + авто-регистрация {@code HomebrewContentItem(ITEM)}; guard владелец + DRAFT; DELETE — 409
 * при использовании (перехват FK-нарушения). createdBy — когда появятся колонки на magic_item (follow-up).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItemAuthoringService {

    private final MagicItemRepository magicItemRepository;
    private final HomebrewAccessService homebrewAccessService;
    private final HomebrewContentItemRepository contentItemRepository;
    private final ContentDictionaryResolver contentDictionaryResolver;

    /**
     * Создаёт предмет в пакете.
     * @param packageId пакет-владелец (DRAFT)
     * @param request тело (kind=MAGIC)
     * @param username автор
     * @return предмет
     */
    @Transactional
    public HomebrewItemResponse create(UUID packageId, HomebrewItemRequest request, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        requireMagic(request);
        MagicItem item = new MagicItem();
        item.setHomebrew(pkg);
        item.setSlug(uniqueSlug(slugify(request.getName()), pkg.getId()));
        apply(item, request, pkg);
        MagicItem saved = magicItemRepository.save(item);
        registerContentItem(pkg, saved.getId());
        log.info("Homebrew magic item created: id={}, packageId={}, by={}", saved.getId(), packageId, username);
        return toResponse(saved);
    }

    /**
     * Обновляет предмет пакета.
     */
    @Transactional
    public HomebrewItemResponse update(UUID packageId, UUID itemId, HomebrewItemRequest request, String username) {
        HomebrewPackage pkg = editablePackage(packageId, username);
        requireMagic(request);
        MagicItem item = requirePackageItem(packageId, itemId);
        apply(item, request, pkg);
        MagicItem saved = magicItemRepository.save(item);
        log.info("Homebrew magic item updated: id={}, packageId={}, by={}", itemId, packageId, username);
        return toResponse(saved);
    }

    /**
     * Читает предмет пакета (для редактора).
     */
    @Transactional(readOnly = true)
    public HomebrewItemResponse get(UUID packageId, UUID itemId, String username) {
        homebrewAccessService.enforceReadable(packageId, username);
        return toResponse(requirePackageItem(packageId, itemId));
    }

    /**
     * Удаляет предмет пакета. 409 при использовании.
     */
    @Transactional
    public void delete(UUID packageId, UUID itemId, String username) {
        editablePackage(packageId, username);
        MagicItem item = requirePackageItem(packageId, itemId);
        contentItemRepository.findAllByHomebrewPackageId(packageId).stream()
                .filter(ci -> ci.getContentType() == ContentType.ITEM && ci.getContentId().equals(itemId))
                .forEach(contentItemRepository::delete);
        try {
            magicItemRepository.delete(item);
            magicItemRepository.flush();
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Предмет используется и не может быть удалён");
        }
        log.info("Homebrew magic item deleted: id={}, packageId={}, by={}", itemId, packageId, username);
    }

    // ================= helpers =================

    private void requireMagic(HomebrewItemRequest request) {
        String kind = request.getKind() == null ? "MAGIC" : request.getKind().toUpperCase(Locale.ROOT);
        if ("TEMPLATE".equals(kind)) {
            throw new BadRequestException("Создание item_templates запрещено (легаси). Используйте MAGIC/EQUIPMENT.");
        }
        if (!"MAGIC".equals(kind)) {
            throw new BadRequestException("Пока поддерживается только kind=MAGIC (EQUIPMENT — следующий шаг)");
        }
    }

    private void apply(MagicItem item, HomebrewItemRequest request, HomebrewPackage pkg) {
        item.setNameRu(request.getName());
        item.setNameEn(request.getNameEn());
        item.setDescription(request.getDescription());
        item.setVariableRarity(false);
        item.setAttunementRequired(Boolean.TRUE.equals(request.getAttunementRequired()));
        item.setAttunementRequirement(request.getAttunementRequirement());
        if (request.getRarity() != null && !request.getRarity().isBlank()) {
            Rarity rarity = contentDictionaryResolver.resolveRarity(request.getRarity(), pkg);
            item.setRarity(rarity);
        } else {
            item.setRarity(null);
        }
    }

    private HomebrewPackage editablePackage(UUID packageId, String username) {
        HomebrewPackage pkg = homebrewAccessService.enforceOwner(packageId, username);
        if (pkg.isDeleted() || pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Предметы можно менять только в DRAFT-пакете");
        }
        return pkg;
    }

    private MagicItem requirePackageItem(UUID packageId, UUID itemId) {
        return magicItemRepository.findByIdAndHomebrew_Id(itemId, packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Предмет не найден в этом пакете"));
    }

    private void registerContentItem(HomebrewPackage pkg, UUID itemId) {
        if (!contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                pkg.getId(), ContentType.ITEM, itemId)) {
            contentItemRepository.save(HomebrewContentItem.builder()
                    .homebrewPackage(pkg)
                    .contentType(ContentType.ITEM)
                    .contentId(itemId)
                    .build());
        }
    }

    private HomebrewItemResponse toResponse(MagicItem item) {
        return HomebrewItemResponse.builder()
                .id(item.getId())
                .kind("MAGIC")
                .name(item.getNameRu())
                .nameEn(item.getNameEn())
                .description(item.getDescription())
                .rarity(item.getRarity() != null ? item.getRarity().getSlug() : null)
                .attunementRequired(item.getAttunementRequired())
                .attunementRequirement(item.getAttunementRequirement())
                .source(item.getHomebrew() != null ? "HOMEBREW" : "GLOBAL")
                .homebrewPackageId(item.getHomebrew() != null ? item.getHomebrew().getId() : null)
                .homebrewPackageTitle(item.getHomebrew() != null ? item.getHomebrew().getTitle() : null)
                .build();
    }

    private String uniqueSlug(String base, UUID packageId) {
        String root = (base == null || base.isBlank())
                ? "item-" + UUID.randomUUID().toString().substring(0, 8) : base;
        String candidate = root;
        int n = 2;
        while (magicItemRepository.existsBySlugAndHomebrew_Id(candidate, packageId)) {
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
        return slug.isBlank() ? "item" : slug;
    }
}
