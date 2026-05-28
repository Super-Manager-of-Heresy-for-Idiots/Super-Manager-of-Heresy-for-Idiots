package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewTag;
import com.dnd.app.repository.HomebrewTagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private static final String TAG_PATTERN = "^[a-z0-9-]{1,50}$";

    private final HomebrewTagRepository tagRepository;

    @Transactional
    public Set<HomebrewTag> findOrCreateTags(List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptySet();
        }

        List<String> normalized = tagNames.stream()
                .map(TagService::normalize)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (normalized.isEmpty()) {
            return Collections.emptySet();
        }

        List<HomebrewTag> existing = tagRepository.findByNameIn(normalized);
        Set<String> existingNames = existing.stream()
                .map(HomebrewTag::getName)
                .collect(Collectors.toSet());

        List<HomebrewTag> created = normalized.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    HomebrewTag tag = HomebrewTag.builder().name(name).build();
                    log.info("Created new tag: '{}'", name);
                    return tagRepository.save(tag);
                })
                .toList();

        Set<HomebrewTag> result = new HashSet<>(existing);
        result.addAll(created);
        return result;
    }

    static String normalize(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().toLowerCase().replaceAll("\\s+", "-");
        if (normalized.isEmpty() || !normalized.matches(TAG_PATTERN)) {
            return null;
        }
        return normalized;
    }
}
