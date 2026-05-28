package com.dnd.app.service.homebrew;

import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.repository.HomebrewContentItemRepository;
import com.dnd.app.repository.HomebrewInstallationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomebrewScopeResolver {

    private final HomebrewInstallationRepository installationRepository;
    private final HomebrewContentItemRepository contentItemRepository;

    public Set<UUID> resolveAccessibleContentIds(UUID gmUserId) {
        Set<UUID> installedPackageIds = installationRepository.findPackageIdsByInstallerId(gmUserId);
        if (installedPackageIds.isEmpty()) {
            return Collections.emptySet();
        }
        return contentItemRepository.findContentIdsByPackageIds(installedPackageIds);
    }

    public Set<UUID> resolveAccessibleContentIds(UUID gmUserId, ContentType contentType) {
        Set<UUID> installedPackageIds = installationRepository.findPackageIdsByInstallerId(gmUserId);
        if (installedPackageIds.isEmpty()) {
            return Collections.emptySet();
        }
        return contentItemRepository.findContentIdsByPackageIdsAndType(installedPackageIds, contentType);
    }
}
