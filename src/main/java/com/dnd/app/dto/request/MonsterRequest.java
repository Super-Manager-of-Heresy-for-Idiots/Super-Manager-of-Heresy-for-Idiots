package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonsterRequest {

    private String slug;

    @NotBlank(message = "nameRusloc is required")
    private String nameRusloc;

    private String nameEngloc;

    private UUID alignmentId;

    @NotBlank(message = "size is required")
    private String size;

    private String sizeSecondary;
    private Boolean isSwarm;
    private String swarmSize;

    @NotNull(message = "armorClass is required")
    private Short armorClass;

    private String armorClassText;
    private Short initiativeBonus;
    private Short initiativeScore;

    private Integer hpAverage;
    private Short hpDiceCount;
    private Short hpDiceSides;
    private Integer hpDiceModifier;
    private String hpFormula;

    @NotNull(message = "strScore is required")
    private Short strScore;
    @NotNull(message = "dexScore is required")
    private Short dexScore;
    @NotNull(message = "conScore is required")
    private Short conScore;
    @NotNull(message = "intScore is required")
    private Short intScore;
    @NotNull(message = "wisScore is required")
    private Short wisScore;
    @NotNull(message = "chaScore is required")
    private Short chaScore;

    private Short passivePerception;
    private Integer telepathyFt;

    @NotBlank(message = "crRating is required")
    private String crRating;

    @NotNull(message = "crValue is required")
    private BigDecimal crValue;

    private Integer xpBase;
    private Integer xpLair;
    private Short proficiencyBonus;
    private Short legendaryUsesBase;
    private Short legendaryUsesLair;
    private String legendaryText;
    private String loreText;

    private Boolean isVisibleToPlayers;
    private Boolean isActive;

    private Set<UUID> creatureTypeIds;
    private Set<UUID> languageIds;
    private Set<UUID> conditionImmunityIds;
    private Set<UUID> habitatIds;
    private Set<UUID> treasureTagIds;
    private Set<UUID> sourceIds;

    @Valid
    private List<SpeedEntry> speeds;
    @Valid
    private List<SenseEntry> senses;
    @Valid
    private List<SavingThrowEntry> savingThrows;
    @Valid
    private List<SkillProficiencyEntry> skillProficiencies;
    @Valid
    private List<DamageEntry> damageResistances;
    @Valid
    private List<DamageEntry> damageImmunities;
    @Valid
    private List<DamageEntry> damageVulnerabilities;
    @Valid
    private List<GearEntry> gear;
    @Valid
    private List<FeatureEntry> features;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpeedEntry {
        @NotNull
        private UUID movementTypeId;
        @NotNull
        private Integer ft;
        private Boolean hover;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SenseEntry {
        @NotNull
        private UUID senseTypeId;
        @NotNull
        private Integer ft;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavingThrowEntry {
        @NotBlank
        private String ability;
        @NotNull
        private Short bonus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillProficiencyEntry {
        @NotNull
        private UUID proficiencySkillId;
        @NotNull
        private Short bonus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamageEntry {
        private String damageType;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GearEntry {
        @NotNull
        private UUID itemId;
        private Short qty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureEntry {
        @NotBlank
        private String section;
        @NotNull
        private Integer sortOrder;
        private String nameRusloc;
        private String nameEngloc;
        @NotBlank
        private String kind;
        private Short rechargeMin;
        private Short rechargeMax;
        @NotBlank
        private String descriptionRusloc;
        private String descriptionEngloc;
        private String attackType;
        private Short attackBonus;
        private Short reachFt;
        private Short rangeFt;
        private Short rangeLongFt;
        private String saveAbility;
        private Short saveDc;
        @Valid
        private List<FeatureDamageEntry> damages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeatureDamageEntry {
        @NotNull
        private Integer sortOrder;
        private Short average;
        private String dice;
        private String damageType;
        private String note;
    }
}
