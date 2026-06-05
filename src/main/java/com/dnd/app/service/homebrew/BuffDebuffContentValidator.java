package com.dnd.app.service.homebrew;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BuffDebuffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BuffDebuffContentValidator implements HomebrewContentValidator {

    private final BuffDebuffRepository buffDebuffRepository;

    @Override
    public String getSupportedType() {
        return "BUFF_DEBUFF";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!buffDebuffRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Бафф/дебафф не найден: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        BuffDebuff buffDebuff = buffDebuffRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(buffDebuff.getId())
                .name(buffDebuff.getName())
                .description(buffDebuff.getDescription())
                .effectType(buffDebuff.getEffectType())
                .isBuff(buffDebuff.getIsBuff())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        BuffDebuff buffDebuff = buffDebuffRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + contentId));
        return buffDebuff.getHomebrew() != null && buffDebuff.getHomebrew().getAuthor() != null
                ? buffDebuff.getHomebrew().getAuthor().getId()
                : null;
    }
}
