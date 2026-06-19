package com.dnd.app.dto.content;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * A single grant attached to a reward group (group-level) or option (option-level).
 * The {@code payload} is a typed discriminated union keyed by {@code grantType}
 * (external property). Unknown grant types deserialize as {@link CustomTextGrantPayload}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RewardGrant", description = "A typed grant within a reward group/option")
public class RewardGrantDto {

    @Schema(description = "Grant id")
    private UUID id;

    @Schema(description = "Grant type. Known: FEATURE | SUBCLASS | FEAT | SPELL | SKILL_PROFICIENCY | "
            + "ABILITY_SCORE | NUMERIC_MODIFIER | CUSTOM_TEXT. Unknown => rendered as custom/manual.",
            example = "ABILITY_SCORE")
    private String grantType;

    @Schema(description = "Display label resolved for locale")
    private String label;
    private String labelRu;
    private String labelEn;

    @Schema(description = "Optional description")
    private String description;

    @Schema(description = "Ordering within its container", example = "0")
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
    @Schema(description = "Typed payload; shape determined by grantType")
    private GrantPayload payload;
}
