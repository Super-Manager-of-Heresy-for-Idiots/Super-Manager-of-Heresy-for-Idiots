package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.HomebrewReport;
import com.dnd.app.domain.HomebrewTag;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.HomebrewReportStatus;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.dto.response.HomebrewPackageResponse;
import com.dnd.app.dto.response.HomebrewReportResponse;
import com.dnd.app.dto.response.HomebrewTagResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.GmHomebrewLibraryRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.HomebrewReportRepository;
import com.dnd.app.repository.HomebrewTagRepository;
import com.dnd.app.repository.UserRepository;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Класс HomebrewAdminService описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewAdminService {

    private final HomebrewPackageRepository packageRepository;
    private final GmHomebrewLibraryRepository gmLibraryRepository;
    private final HomebrewTagRepository tagRepository;
    private final HomebrewReportRepository reportRepository;
    private final UserRepository userRepository;
    private final HomebrewAuthoringService authoringService;

    /**
     * Возвращает список для операции "list all packages" в рамках бизнес-логики homebrew-контента.
     * @param status входящее значение status, используемое бизнес-сценарием
     * @param authorId идентификатор author, используемый для выбора нужного бизнес-объекта
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Page<HomebrewPackageResponse> listAllPackages(String status, UUID authorId, Pageable pageable) {
        Page<HomebrewPackage> packages;
        if (status != null && authorId != null) {
            HomebrewStatus s = HomebrewStatus.valueOf(status.toUpperCase());
            packages = packageRepository.findAllByAuthorIdAndStatus(authorId, s, pageable);
        } else if (status != null) {
            HomebrewStatus s = HomebrewStatus.valueOf(status.toUpperCase());
            packages = packageRepository.findAllByStatus(s, pageable);
        } else if (authorId != null) {
            packages = packageRepository.findAllByAuthorId(authorId, pageable);
        } else {
            packages = packageRepository.findAll(pageable);
        }
        return packages.map(authoringService::toPackageResponse);
    }

    /**
     * Выполняет операции "hard delete" в рамках бизнес-логики homebrew-контента.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public Map<String, Object> hardDelete(UUID id) {
        HomebrewPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        long affectedLibraryEntries = gmLibraryRepository.countByPackageId(id);
        log.info("Admin hard-deleting package: id={}, title='{}', author={}, libraryEntries={}",
                pkg.getId(), pkg.getTitle(), pkg.getAuthor().getUsername(), affectedLibraryEntries);

        gmLibraryRepository.deleteByPackageId(id);
        packageRepository.delete(pkg);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedPackageId", id);
        result.put("affectedLibraryEntries", affectedLibraryEntries);
        return result;
    }

    /**
     * Возвращает список для операции "list tags with usage count" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<HomebrewTagResponse> listTagsWithUsageCount() {
        List<Object[]> rows = tagRepository.findAllWithUsageCount();
        return rows.stream()
                .map(row -> HomebrewTagResponse.builder()
                        .id((UUID) row[0])
                        .name((String) row[1])
                        .usageCount(((Number) row[2]).longValue())
                        .build())
                .toList();
    }

    /**
     * Удаляет результат операции "delete tag" в рамках бизнес-логики homebrew-контента.
     * @param tagId идентификатор tag, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void deleteTag(UUID tagId) {
        HomebrewTag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Тег не найден"));

        long usageCount = packageRepository.findAll().stream()
                .filter(p -> p.getTags().contains(tag))
                .count();
        if (usageCount > 0) {
            throw new DuplicateResourceException("Тег все еще используется в " + usageCount + " пакетах");
        }

        tagRepository.delete(tag);
        log.info("Admin deleted tag: id={}, name='{}'", tagId, tag.getName());
    }

    // ===================== P2-6: пост-модерация =====================

    /**
     * Очередь жалоб для админ-модерации.
     * @param status фильтр по статусу жалобы (OPEN/RESOLVED/DISMISSED); null — все
     * @param pageable постраничность
     * @return страница жалоб
     */
    @Transactional(readOnly = true)
    public Page<HomebrewReportResponse> listReports(String status, Pageable pageable) {
        Page<HomebrewReport> reports;
        if (status != null && !status.isBlank()) {
            HomebrewReportStatus st = parseReportStatus(status);
            reports = reportRepository.findAllByStatusOrderByCreatedAtAsc(st, pageable);
        } else {
            reports = reportRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return reports.map(this::toReportResponse);
    }

    /**
     * Отклонить пакет модератором: статус REJECTED (скрыт с витрины, недоступен для install/attach).
     * Все открытые жалобы по пакету помечаются RESOLVED.
     * @param id идентификатор пакета
     * @param reason причина отклонения (для аудита; может быть null)
     * @param adminUsername модератор
     * @return ответ по пакету
     */
    @Transactional
    public HomebrewPackageResponse rejectPackage(UUID id, String reason, String adminUsername) {
        HomebrewPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        pkg.setStatus(HomebrewStatus.REJECTED);
        packageRepository.save(pkg);

        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        resolveOpenReports(id, admin);

        log.info("MODERATION: package REJECTED: id={}, title='{}', by={}, reason='{}'",
                id, pkg.getTitle(), adminUsername, reason);
        return authoringService.toPackageResponse(pkg);
    }

    /**
     * Восстановить отклонённый/архивный пакет обратно в PUBLISHED.
     * @param id идентификатор пакета
     * @param adminUsername модератор
     * @return ответ по пакету
     */
    @Transactional
    public HomebrewPackageResponse restorePackage(UUID id, String adminUsername) {
        HomebrewPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        if (pkg.getStatus() != HomebrewStatus.REJECTED && pkg.getStatus() != HomebrewStatus.ARCHIVED) {
            throw new BadRequestException("Восстановить можно только отклонённый или архивный пакет");
        }
        pkg.setStatus(HomebrewStatus.PUBLISHED);
        packageRepository.save(pkg);
        log.info("MODERATION: package RESTORED to PUBLISHED: id={}, title='{}', by={}",
                id, pkg.getTitle(), adminUsername);
        return authoringService.toPackageResponse(pkg);
    }

    /**
     * Обработать жалобу без отклонения пакета: DISMISS (необоснованна) или RESOLVE (учтена).
     * @param reportId идентификатор жалобы
     * @param action DISMISS | RESOLVE
     * @param adminUsername модератор
     * @return обновлённая жалоба
     */
    @Transactional
    public HomebrewReportResponse resolveReport(UUID reportId, String action, String adminUsername) {
        HomebrewReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Жалоба не найдена"));
        HomebrewReportStatus target = switch (action == null ? "" : action.toUpperCase()) {
            case "DISMISS" -> HomebrewReportStatus.DISMISSED;
            case "RESOLVE" -> HomebrewReportStatus.RESOLVED;
            default -> throw new BadRequestException("Недопустимое действие: " + action + ". Допустимо: DISMISS, RESOLVE");
        };
        report.setStatus(target);
        report.setResolvedAt(Instant.now());
        report.setResolvedBy(userRepository.findByUsername(adminUsername).orElse(null));
        reportRepository.save(report);
        log.info("MODERATION: report {} -> {}, by={}", reportId, target, adminUsername);
        return toReportResponse(report);
    }

    private void resolveOpenReports(UUID packageId, User admin) {
        for (HomebrewReport report : reportRepository.findAllByHomebrewPackageIdAndStatus(packageId, HomebrewReportStatus.OPEN)) {
            report.setStatus(HomebrewReportStatus.RESOLVED);
            report.setResolvedAt(Instant.now());
            report.setResolvedBy(admin);
            reportRepository.save(report);
        }
    }

    private HomebrewReportStatus parseReportStatus(String status) {
        try {
            return HomebrewReportStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный статус жалобы: " + status);
        }
    }

    private HomebrewReportResponse toReportResponse(HomebrewReport r) {
        HomebrewPackage pkg = r.getHomebrewPackage();
        return HomebrewReportResponse.builder()
                .id(r.getId())
                .packageId(pkg.getId())
                .packageTitle(pkg.getTitle())
                .packageStatus(pkg.getStatus().name())
                .reporterUsername(r.getReporter() != null ? r.getReporter().getUsername() : null)
                .reason(r.getReason())
                .status(r.getStatus().name())
                .createdAt(r.getCreatedAt())
                .resolvedAt(r.getResolvedAt())
                .resolvedByUsername(r.getResolvedBy() != null ? r.getResolvedBy().getUsername() : null)
                .build();
    }
}
