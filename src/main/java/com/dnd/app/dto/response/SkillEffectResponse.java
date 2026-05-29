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
public class SkillEffectResponse {
    private UUID id;
    private BuffDebuffResponse buffDebuff;
    private String effectRole;
    private Integer chancePercent;
}
