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
public class ProficiencySkillResponse {
    private UUID id;
    private String name;
    private UUID governingStatId;
    private String governingStatName;
}
