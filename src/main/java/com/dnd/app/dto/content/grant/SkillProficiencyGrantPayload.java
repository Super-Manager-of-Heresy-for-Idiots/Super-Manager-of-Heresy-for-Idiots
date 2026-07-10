package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс SkillProficiencyGrantPayload описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SkillProficiencyGrantPayload",
        description = "Grants/chooses skill proficiencies (payload for grantType=SKILL_PROFICIENCY)")
public class SkillProficiencyGrantPayload implements GrantPayload {

    @Schema(description = "FIXED => grant specific skills; CHOICE => pick from pool; ANY => pick from all",
            example = "CHOICE", allowableValues = {"FIXED", "CHOICE", "ANY"})
    private String mode;

    // mode = FIXED
    @Schema(description = "Skills to grant (mode=FIXED)")
    private List<UUID> skillIds;

    // mode = CHOICE
    @Schema(description = "Pool of selectable skills (mode=CHOICE)")
    private List<UUID> skillOptionIds;

    // mode = CHOICE | ANY
    @Schema(description = "How many skills to choose", example = "2")
    private Integer chooseCount;

    @Schema(description = "When true, this grant confers Expertise (doubled proficiency bonus) and the "
            + "chosen skills must be ones the character is already proficient in.", example = "true")
    private Boolean grantsExpertise;
}
