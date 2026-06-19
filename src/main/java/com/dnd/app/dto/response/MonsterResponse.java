package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MonsterResponse {

    private UUID id;
    private Long sourceExternalId;
    private String slug;
    private String nameRusloc;
    private String nameEngloc;

    private DictionaryRef alignment;
    private DictionaryRef size;
    private DictionaryRef sizeSecondary;
    private Boolean isSwarm;
    private DictionaryRef swarmSize;

    private Short armorClass;
    private String armorClassText;
    private Short initiativeBonus;
    private Short initiativeScore;

    private Integer hpAverage;
    private Short hpDiceCount;
    private Short hpDiceSides;
    private Integer hpDiceModifier;
    private String hpFormula;

    private Short strScore;
    private Short dexScore;
    private Short conScore;
    private Short intScore;
    private Short wisScore;
    private Short chaScore;

    private Short passivePerception;
    private Integer telepathyFt;

    private String crRating;
    private BigDecimal crValue;
    private Integer xpBase;
    private Integer xpLair;
    private Short proficiencyBonus;
    private Short legendaryUsesBase;
    private Short legendaryUsesLair;
    private String legendaryText;
    private String loreText;

    private String scope;
    private UUID homebrewId;
    private UUID campaignId;
    private UUID sourceMonsterId;
    private Boolean isVisibleToPlayers;
    private Boolean isActive;

    private UUID createdBy;
    private String createdByUsername;
    private UUID updatedBy;
    private String updatedByUsername;

    private Instant createdAt;
    private Instant updatedAt;

    private List<DictionaryRef> creatureTypes;
    private List<DictionaryRef> languages;
    private List<DictionaryRef> conditionImmunities;
    private List<DictionaryRef> habitats;
    private List<DictionaryRef> treasureTags;
    private List<DictionaryRef> sources;

    private List<SpeedView> speeds;
    private List<SenseView> senses;
    private List<SavingThrowView> savingThrows;
    private List<SkillProficiencyView> skillProficiencies;
    private List<DamageView> damageResistances;
    private List<DamageView> damageImmunities;
    private List<DamageView> damageVulnerabilities;
    private List<GearView> gear;
    private List<FeatureView> features;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DictionaryRef {
        private UUID id;
        private String code;
        private String nameRusloc;
        private String nameEngloc;
        private UUID homebrewId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SpeedView {
        private UUID id;
        private DictionaryRef movementType;
        private Integer ft;
        private Boolean hover;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SenseView {
        private UUID id;
        private DictionaryRef senseType;
        private Integer ft;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SavingThrowView {
        private UUID id;
        private DictionaryRef ability;
        private Short bonus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SkillProficiencyView {
        private UUID id;
        private UUID proficiencySkillId;
        private String skillName;
        private Short bonus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DamageView {
        private UUID id;
        private DictionaryRef damageType;
        private String note;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class GearView {
        private UUID id;
        private DictionaryRef item;
        private Short qty;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeatureView {
        private UUID id;
        private String section;
        private Integer sortOrder;
        private String nameRusloc;
        private String nameEngloc;
        private String kind;
        private Short rechargeMin;
        private Short rechargeMax;
        private String descriptionRusloc;
        private String descriptionEngloc;
        private String attackType;
        private Short attackBonus;
        private Short reachFt;
        private Short rangeFt;
        private Short rangeLongFt;
        private DictionaryRef saveAbility;
        private Short saveDc;
        private List<FeatureDamageView> damages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class FeatureDamageView {
        private UUID id;
        private Integer sortOrder;
        private Short average;
        private String dice;
        private DictionaryRef damageType;
        private String note;
    }
}
