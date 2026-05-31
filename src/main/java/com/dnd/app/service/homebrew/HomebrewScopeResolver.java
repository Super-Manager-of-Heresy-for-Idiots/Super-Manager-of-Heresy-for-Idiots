package com.dnd.app.service.homebrew;

import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.repository.GmHomebrewLibraryRepository;
import com.dnd.app.repository.HomebrewContentItemRepository;
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

    private final GmHomebrewLibraryRepository gmLibraryRepository;
    private final HomebrewContentItemRepository contentItemRepository;

    public Set<UUID> resolveAccessibleContentIds(UUID gmUserId) {
        Set<UUID> installedPackageIds = gmLibraryRepository.findPackageIdsByGmUserId(gmUserId);
        if (installedPackageIds.isEmpty()) {
            return Collections.emptySet();
        }
        return contentItemRepository.findContentIdsByPackageIds(installedPackageIds);
    }

    public Set<UUID> resolveAccessibleContentIds(UUID gmUserId, ContentType contentType) {
        Set<UUID> installedPackageIds = gmLibraryRepository.findPackageIdsByGmUserId(gmUserId);
        if (installedPackageIds.isEmpty()) {
            return Collections.emptySet();
        }
        return contentItemRepository.findContentIdsByPackageIdsAndType(installedPackageIds, contentType);
    }
}
