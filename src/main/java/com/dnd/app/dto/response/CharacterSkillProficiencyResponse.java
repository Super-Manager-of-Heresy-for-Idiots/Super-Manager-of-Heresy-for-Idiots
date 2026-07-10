package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CharacterSkillProficiencyResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterSkillProficiencyResponse {
    private UUID skillId;
    private String skillName;
    private String source;
    /** PROFICIENT or EXPERTISE. EXPERTISE means the proficiency bonus is doubled for this skill. */
    private String proficiencyLevel;
}
