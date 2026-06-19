package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * SKILL_PROFICIENCY grant — grants fixed skills, a choice from a pool, or any skill.
 * Reference-only to {@code skill}.
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
}
