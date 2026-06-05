package com.dnd.app.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class RaceTraitRequest {

    private UUID id;

    @NotBlank(message = "Trait name is required")
    @Size(max = 100, message = "Trait name must be <= 100 characters")
    private String name;

    private String description;

    @Min(value = 1, message = "Level requirement must be >= 1")
    private Integer levelRequirement;

    @Valid
    private UsesDto uses;

    private String actionType;

    @Valid
    private DamageDto damage;

    @Valid
    private SavingThrowDto savingThrow;

    private JsonNode grantedSpells;
    private JsonNode innateSpells;
    private JsonNode metadata;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsesDto {
        private String type;
        private String recharge;
        private String amountExpression;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamageDto {
        private String diceExpression;
        private String damageType;
        private String scaling;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavingThrowDto {
        private String ability;
        private String dcFormula;
    }
}
