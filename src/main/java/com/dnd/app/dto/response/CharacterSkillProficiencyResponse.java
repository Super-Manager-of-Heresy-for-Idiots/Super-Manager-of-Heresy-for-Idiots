package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

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
