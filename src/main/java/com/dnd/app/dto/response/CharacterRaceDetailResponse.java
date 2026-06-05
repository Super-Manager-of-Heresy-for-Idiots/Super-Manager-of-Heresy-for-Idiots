package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterRaceDetailResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer speed;
    private List<AbilityScoreIncrease> abilityScoreIncreases;
    private List<String> traits;
    private List<SubraceInfo> subraces;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityScoreIncrease {
        private String statName;
        private Integer bonus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubraceInfo {
        private UUID id;
        private String name;
        private String description;
        private List<AbilityScoreIncrease> abilityScoreIncreases;
        private Integer speedOverride;
        private List<String> traits;
    }
}
