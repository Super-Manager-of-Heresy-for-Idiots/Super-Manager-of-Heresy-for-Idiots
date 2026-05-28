package com.dnd.app.service.homebrew;

import com.dnd.app.dto.response.ContentSummaryDto;

import java.util.UUID;

public interface HomebrewContentValidator {
    String getSupportedType();
    void validateExists(UUID contentId);
    ContentSummaryDto summarize(UUID contentId);
    UUID getOwnerId(UUID contentId);
}
