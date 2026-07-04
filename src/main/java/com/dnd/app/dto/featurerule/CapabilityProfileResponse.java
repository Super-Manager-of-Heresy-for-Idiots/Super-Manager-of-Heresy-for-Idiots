package com.dnd.app.dto.featurerule;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Per-character "capability profile": the single source of truth the frontend uses to decide which
 * class-aware panels/tabs/sections to render (spells, spellbook, resources, actions, effects, wild-shape
 * forms, companions, spell grants, pending choices/prompts). See
 * {@code docs/FEATURE_RULES_FRONTEND_REWORK_PLAN.md} §0.
 *
 * <p>The {@link #spellcasting} block is derived from class content ({@code character_class}) and is always
 * populated (it is independent of the feature-rules runtime flags), so gating the spells tab by
 * {@code spellcasting.isCaster} works even when the runtime is off. All feature-rules presence flags below
 * are only computed when the corresponding {@code app.feature-rules.*} subsystem is active; otherwise they
 * are {@code false}/{@code 0} so the profile always reflects what the backend will actually serve.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CapabilityProfile", description = "Class-aware capability profile driving the character UI")
public class CapabilityProfileResponse {

    private UUID characterId;
    private Integer totalLevel;
    private Integer proficiencyBonus;

    @Schema(description = "Whether the feature-rules runtime master switch is enabled in this deployment")
    private boolean runtimeEnabled;

    @Schema(description = "Spellcasting capability (isCaster=false => a non-caster like Barbarian/Monk/Rogue)")
    private SpellcastingCapability spellcasting;

    @Schema(description = "Per-class breakdown (for multiclass-aware UI)")
    private List<ClassCapability> classes;

    // ── Feature-rules runtime presence (only set when the matching subsystem is active) ──────────
    private boolean hasFeatureResources;
    private boolean hasFeatureActions;
    private boolean hasActiveEffects;
    private boolean hasCompanions;
    private boolean hasFeatureSpellGrants;

    @Schema(description = "Wild-shape / known-forms capability; null when the class cannot transform")
    private WildShapeCapability wildShape;

    @Schema(description = "Count of unresolved feature choices (Fighting Style, Expertise, Metamagic, ...)")
    private int pendingChoices;

    @Schema(description = "Count of durable pending gameplay prompts (reactions, etc.)")
    private int pendingPrompts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "SpellcastingCapability")
    public static class SpellcastingCapability {
        @Schema(description = "True if any of the character's classes is a spellcaster (JSON: 'caster')")
        private boolean caster;
        @Schema(description = "FULL | HALF | MULTI | NONE. THIRD/PACT are not yet distinguishable from " +
                "class content and are reported as FULL/HALF; enrich later via a content column.")
        private String casterType;
        @Schema(description = "True if the (primary) caster class learns cantrips")
        private boolean hasCantrips;
        private UUID abilityId;
        private String abilityNameRu;
        private String abilityNameEn;
        @Schema(description = "8 + proficiency bonus + spellcasting ability modifier; null if unknown")
        private Integer spellSaveDc;
        @Schema(description = "proficiency bonus + spellcasting ability modifier; null if unknown")
        private Integer spellAttackBonus;
        @Schema(description = "Preparation model: PREPARED | KNOWN | null (from class content)")
        private String preparation;
        @Schema(description = "True if the (primary) caster class records spells in a spellbook (Wizard)")
        private boolean usesSpellbook;
        @Schema(description = "True if the (primary) caster class can ritual-cast")
        private boolean ritual;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "WildShapeCapability")
    public static class WildShapeCapability {
        @Schema(description = "True if the class has a form-granting feature (so show the forms panel " +
                "even before any form is learned)")
        private boolean canWildShape;
        private int knownFormCount;
        private boolean activeTransformation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ClassCapability")
    public static class ClassCapability {
        private UUID classId;
        private String classNameRu;
        private String classNameEn;
        private Integer classLevel;
        private boolean caster;
        private String casterType;
    }
}
