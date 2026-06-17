package com.dnd.app.dto.request;

import com.dnd.app.dto.content.grant.AbilityScoreGrantPayload;
import com.dnd.app.dto.content.grant.CustomTextGrantPayload;
import com.dnd.app.dto.content.grant.FeatGrantPayload;
import com.dnd.app.dto.content.grant.FeatureGrantPayload;
import com.dnd.app.dto.content.grant.GrantPayload;
import com.dnd.app.dto.content.grant.NumericModifierGrantPayload;
import com.dnd.app.dto.content.grant.SkillProficiencyGrantPayload;
import com.dnd.app.dto.content.grant.SpellGrantPayload;
import com.dnd.app.dto.content.grant.SubclassGrantPayload;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Aggregate class-authoring request (create or update). One request describes the whole
 * class graph (mechanics + features + subclasses + reward groups → options → grants).
 * Children carry {@code id} (update existing) or {@code key} (create new; grants can
 * reference not-yet-saved features/subclasses by key). See class-authoring-contract.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassWriteRequest", description = "Aggregate create/update of a whole class graph")
public class ClassWriteRequest {

    @NotBlank(message = "Название класса обязательно")
    private String name;
    private String nameRu;
    private String nameEn;
    private String slug;
    private String subtitle;
    private String description;

    @NotNull(message = "Кость хитов обязательна")
    @Schema(description = "6 | 8 | 10 | 12")
    private Integer hitDie;

    @Schema(description = "ability_score ids; >= 1")
    private List<UUID> primaryAbilityIds;
    @Schema(description = "ability_score ids (обычно 2)")
    private List<UUID> savingThrowIds;

    private Integer skillChoiceCount;
    private Boolean skillChoiceAny;
    @Schema(description = "skill ids; пул, если skillChoiceAny=false")
    private List<UUID> skillOptionIds;

    private String armorProficiencyText;
    private String weaponProficiencyText;
    private String toolProficiencyText;

    @Valid
    private SpellcastingProfile spellcasting;

    @Valid
    private List<FeatureInput> features;
    @Valid
    private List<SubclassInput> subclasses;
    @Valid
    private List<RewardGroupInput> rewardGroups;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpellcastingProfile")
    public static class SpellcastingProfile {
        private String casterProgression;
        private UUID spellcastingAbilityId;
        private String preparation;
        private Boolean ritualCasting;
        private Boolean hasCantrips;
        private String spellcastingFocusText;
        private String notes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "FeatureInput")
    public static class FeatureInput {
        private UUID id;
        private String key;
        private Integer level;
        private Integer sortOrder;
        @NotBlank(message = "Заголовок фичи обязателен")
        private String title;
        private String description;
        private UUID subclassId;
        private String subclassKey;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SubclassInput")
    public static class SubclassInput {
        private UUID id;
        private String key;
        @NotBlank(message = "Имя сабкласса обязательно")
        private String name;
        private String nameRu;
        private String nameEn;
        private String slug;
        private String subtitle;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RewardGroupInput")
    public static class RewardGroupInput {
        private UUID id;
        private String key;
        @NotNull
        private Integer classLevel;
        @NotBlank
        @Schema(description = "AUTO | CHOICE")
        private String groupKind;
        private String prompt;
        private String description;
        private Integer chooseMin;
        private Integer chooseMax;
        private Boolean repeatable;
        private Integer sortOrder;
        private UUID classFeatureId;
        private String classFeatureKey;
        @Valid
        private List<RewardOptionInput> options;
        @Valid
        private List<GrantInput> grants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "RewardOptionInput")
    public static class RewardOptionInput {
        private UUID id;
        private String key;
        private String optionKey;
        @NotBlank
        private String label;
        private String labelRu;
        private String labelEn;
        private String description;
        private Boolean recommended;
        private Integer sortOrder;
        @Valid
        private List<GrantInput> grants;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "GrantInput")
    public static class GrantInput {
        private UUID id;
        @NotBlank
        private String grantType;
        private String label;
        private String labelRu;
        private String labelEn;
        private String description;
        private Integer sortOrder;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "grantType",
                visible = true,
                defaultImpl = CustomTextGrantPayload.class)
        @JsonSubTypes({
                @JsonSubTypes.Type(value = FeatureGrantPayload.class, name = "FEATURE"),
                @JsonSubTypes.Type(value = SubclassGrantPayload.class, name = "SUBCLASS"),
                @JsonSubTypes.Type(value = FeatGrantPayload.class, name = "FEAT"),
                @JsonSubTypes.Type(value = SpellGrantPayload.class, name = "SPELL"),
                @JsonSubTypes.Type(value = SkillProficiencyGrantPayload.class, name = "SKILL_PROFICIENCY"),
                @JsonSubTypes.Type(value = AbilityScoreGrantPayload.class, name = "ABILITY_SCORE"),
                @JsonSubTypes.Type(value = NumericModifierGrantPayload.class, name = "NUMERIC_MODIFIER"),
                @JsonSubTypes.Type(value = CustomTextGrantPayload.class, name = "CUSTOM_TEXT")
        })
        private GrantPayload payload;
    }
}
