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
public class CharacterClassDetailResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer hitDie;
    private UUID primaryAbilityStatId;
    private List<String> savingThrowStatNames;
    private Integer skillChoiceCount;
    private List<ProficiencySkillResponse> skillChoiceOptions;
    private String armorWeaponProficiencies;
    private SpellcastingInfo spellcasting;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpellcastingInfo {
        private Boolean isSpellcaster;
        private UUID spellcastingStatId;
        private String spellcastingStatName;
        private Boolean hasCantrips;
        private Boolean isHalfCaster;
    }
}
