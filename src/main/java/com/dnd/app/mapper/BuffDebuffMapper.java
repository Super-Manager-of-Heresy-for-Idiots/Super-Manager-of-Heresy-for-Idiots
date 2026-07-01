package com.dnd.app.mapper;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.dto.response.BuffDebuffResponse;

public final class BuffDebuffMapper {

    private BuffDebuffMapper() {
    }

    public static BuffDebuffResponse toResponse(BuffDebuff bd) {
        if (bd == null) {
            return null;
        }

        return BuffDebuffResponse.builder()
                .id(bd.getId())
                .name(bd.getName())
                .description(bd.getDescription())
                .effectType(bd.getEffectType())
                .targetStatId(bd.getTargetStat() != null ? bd.getTargetStat().getId() : null)
                .targetStatName(bd.getTargetStat() != null ? bd.getTargetStat().getNameRu() : null)
                .modifierValue(bd.getModifierValue())
                .durationRounds(bd.getDurationRounds())
                .isBuff(bd.getIsBuff())
                .createdAt(bd.getCreatedAt())
                .build();
    }
}
