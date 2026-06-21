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

    @Min(1)
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

    // --- character-sheet narrative / combat fields (persisted on the character) ---

    @Size(max = 40)
    @Schema(description = "Moral alignment, e.g. \"Lawful Good\"")
    private String alignment;

    @Schema(description = "Portrait reference (URL or data URI)")
    private String avatarUrl;

    @Schema(description = "Free-form proficiencies & languages text")
    private String proficiencies;

    @Schema(description = "Free-form equipment text")
    private String equipment;

    @Schema(description = "Free-form features & traits text")
    private String features;

    @Valid
    @Schema(description = "Personality traits, ideals, bonds and flaws")
    private Biography biography;

    @Valid
    @Schema(description = "Custom attacks / strikes")
    private List<Attack> attacks;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContentBiography")
    public static class Biography {
        @Size(max = 2000)
        private String personalityTraits;
        @Size(max = 2000)
        private String ideals;
        @Size(max = 2000)
        private String bonds;
        @Size(max = 2000)
        private String flaws;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ContentAttack")
    public static class Attack {
        @Size(max = 100)
        private String name;
        @Size(max = 20)
        private String attackBonus;
        @Size(max = 50)
        private String damage;
        @Size(max = 50)
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
