package com.dnd.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.dnd.app.dto.content.LevelUpRequest;

import java.util.List;
import java.util.UUID;

/**
 * Character creation request against the NEW content model. All content references
 * resolve to the normalized tables: {@code classId} -> character_class,
 * {@code chosenSkillIds} -> skill, {@code backgroundId} -> background,
 * {@code cantripIds}/{@code spellIds} -> spell, {@code startingCoins} -> currency.
 * Created characters store these new content IDs (see Phase 5 storage decision).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreateContentCharacterRequest", description = "Create a character from the new content model")
public class CreateContentCharacterRequest {

    @NotBlank(message = "Имя персонажа обязательно")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String playerName;

    @NotNull(message = "ID класса обязателен")
    @Schema(description = "character_class id")
    private UUID classId;

    @NotNull(message = "ID расы обязателен")
    private UUID raceId;

    private UUID selectedLineageId;

    @NotNull(message = "ID предыстории обязателен")
    @Schema(description = "background id")
    private UUID backgroundId;

    // Target level. Values below 1 are clamped to 1 by the creation service
    // (the character is always created at level 1 and granted XP for the target level).
    @Max(20)
    @Builder.Default
    private int level = 1;

    @NotNull(message = "Характеристики обязательны")
    @Size(min = 6, max = 6, message = "Должно быть ровно 6 характеристик")
    @Valid
    private List<AbilityScoreEntry> abilityScores;

    @NotNull(message = "Метод распределения характеристик обязателен")
    @Schema(description = "STANDARD_ARRAY | POINT_BUY | ROLL")
    private String scoreMethod;

    @Schema(description = "Chosen class skill proficiencies (skill ids), validated against class_skill_option")
    private List<UUID> chosenSkillIds;

    @Schema(description = "Cantrip spell ids (level 0)")
    private List<UUID> cantripIds;

    @Schema(description = "Leveled spell ids (level 1+)")
    private List<UUID> spellIds;

    private List<StartingCoin> startingCoins;

    @Valid
    @Schema(description = "Initial level-1 reward-group selections")
    private List<LevelUpRequest.GroupSelection> initialRewardSelections;

    @Schema(description = "Free-form proficiencies & languages text")
    private String proficiencies;

    @Schema(description = "Free-form equipment / inventory text")
    private String equipment;

    @Schema(description = "Free-form features & traits text (description, backstory, class/species features)")
    private String features;

    @Size(max = 40)
    @Schema(description = "Alignment, e.g. 'Lawful Good'")
    private String alignment;

    @Valid
    @Schema(description = "Personality traits, ideals, bonds, flaws")
    private BiographyEntry biography;

    @Valid
    @Schema(description = "Weapon / spell attacks")
    private List<AttackEntry> attacks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContentBiographyEntry")
    public static class BiographyEntry {
        private String personalityTraits;
        private String ideals;
        private String bonds;
        private String flaws;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContentAttackEntry")
    public static class AttackEntry {
        private String name;
        private String attackBonus;
        private String damage;
        private String damageType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContentAbilityScoreEntry")
    public static class AbilityScoreEntry {
        @NotNull
        @Schema(description = "ability_score id")
        private UUID statId;
        @Min(1)
        @Max(30)
        private int baseValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContentStartingCoin")
    public static class StartingCoin {
        @NotNull
        @Schema(description = "currency id")
        private UUID currencyTypeId;
        @Min(0)
        private int amount;
    }
}
