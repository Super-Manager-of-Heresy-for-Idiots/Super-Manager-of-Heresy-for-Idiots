package com.dnd.app.service.homebrew;

import com.dnd.app.dto.response.ContentSummaryDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class HomebrewContentValidatorRegistry {

    private final Map<String, HomebrewContentValidator> validators;

    public HomebrewContentValidatorRegistry(List<HomebrewContentValidator> validatorList) {
        this.validators = validatorList.stream()
                .collect(Collectors.toMap(HomebrewContentValidator::getSupportedType, Function.identity()));
        log.info("HomebrewContentValidatorRegistry initialized with types: {}", validators.keySet());
    }

    public void validate(String contentType, UUID contentId) {
        getValidator(contentType).validateExists(contentId);
    }

    public ContentSummaryDto summarize(String contentType, UUID contentId) {
        return getValidator(contentType).summarize(contentId);
    }

    public UUID getOwnerId(String contentType, UUID contentId) {
        return getValidator(contentType).getOwnerId(contentId);
    }

    public boolean isKnownType(String contentType) {
        return validators.containsKey(contentType);
    }

    public Set<String> getSupportedTypes() {
        return validators.keySet();
    }

    private HomebrewContentValidator getValidator(String contentType) {
        HomebrewContentValidator validator = validators.get(contentType);
        if (validator == null) {
            log.error("Unknown content type: '{}', available types: {}", contentType, validators.keySet());
            throw new IllegalArgumentException("Неизвестный тип контента: " + contentType);
        }
        return validator;
    }
}
