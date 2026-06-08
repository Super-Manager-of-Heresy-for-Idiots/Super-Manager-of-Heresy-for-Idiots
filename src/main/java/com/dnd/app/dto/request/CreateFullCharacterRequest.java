package com.dnd.app.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFullCharacterRequest {

    @NotBlank(message = "Имя персонажа обязательно")
    @Size(max = 100)
    private String name;

    @Size(max = 100)
    private String playerName;

    private String proficiencies;

    private String equipment;

    private String alignment;

    private String avatar;

    @NotNull(message = "ID расы обязателен")
    private UUID raceId;

    private UUID subraceId;

    @NotNull(message = "ID класса обязателен")
    private UUID classId;

    @Min(1) @Max(20)
    private int level;

    @NotNull(message = "Характеристики обязательны")
    @Size(min = 6, max = 6, message = "Должно быть ровно 6 характеристик")
    private List<AbilityScoreEntry> abilityScores;

    @NotNull(message = "Метод распределения характеристик обязателен")
    private String scoreMethod;

    @NotNull(message = "ID предыстории обязателен")
    private UUID backgroundId;

    @NotNull(message = "Навыки класса обязательны")
    private List<UUID> chosenSkillProficiencyIds;

    private List<UUID> cantripIds;
    private List<UUID> spellIds;

    private BiographyDto biography;

    private List<StartingCoin> startingCoins;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityScoreEntry {
        @NotNull
        private UUID statId;
        @Min(1) @Max(30)
        private int baseValue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BiographyDto {
        private String personalityTraits;
        private String ideals;
        private String bonds;
        private String flaws;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartingCoin {
        @NotNull
        private UUID currencyTypeId;
        @Min(0)
        private int amount;
    }
}
