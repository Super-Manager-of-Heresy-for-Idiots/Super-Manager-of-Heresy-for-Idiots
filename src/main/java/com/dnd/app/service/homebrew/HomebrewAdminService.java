package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.HomebrewTag;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.dto.response.HomebrewPackageResponse;
import com.dnd.app.dto.response.HomebrewTagResponse;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.HomebrewInstallationRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.HomebrewTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewAdminService {

    private final HomebrewPackageRepository packageRepository;
    private final HomebrewInstallationRepository installationRepository;
    private final HomebrewTagRepository tagRepository;
    private final HomebrewAuthoringService authoringService;

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

    @Transactional
    public Map<String, Object> hardDelete(UUID id) {
        HomebrewPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        long affectedInstallations = installationRepository.countByHomebrewPackageId(id);
        log.info("Admin hard-deleting package: id={}, title='{}', author={}, installCount={}",
                pkg.getId(), pkg.getTitle(), pkg.getAuthor().getUsername(), affectedInstallations);

        installationRepository.findAll().stream()
                .filter(i -> i.getHomebrewPackage().getId().equals(id))
                .forEach(installationRepository::delete);

        packageRepository.delete(pkg);

        Map<String, Object> result = new HashMap<>();
        result.put("deletedPackageId", id);
        result.put("affectedInstallations", affectedInstallations);
        return result;
    }

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
}
