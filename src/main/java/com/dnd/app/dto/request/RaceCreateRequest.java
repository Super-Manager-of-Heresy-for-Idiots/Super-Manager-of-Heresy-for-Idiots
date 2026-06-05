package com.dnd.app.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceCreateRequest {

    @NotBlank(message = "Race name is required")
    @Size(max = 50, message = "Race name must be <= 50 characters")
    private String name;

    @Size(max = 80, message = "Slug must be <= 80 characters")
    private String slug;

    private String description;
    private String loreDescription;

    @NotBlank(message = "Source type is required")
    private String sourceType;

    private String sourceName;

    private Boolean active;

    @NotBlank(message = "Creature type is required")
    private String creatureType;

    @NotEmpty(message = "At least one size option is required")
    private List<String> sizeOptions;

    private String defaultSize;

    @Valid
    @NotNull(message = "Speed is required")
    private RaceSpeedDto speed;

    @Min(value = 0, message = "Darkvision range must be >= 0")
    private Integer darkvisionRange;

    @Valid
    private List<RaceTraitRequest> traits;

    @Valid
    private List<RaceLineageRequest> lineageOptions;

    private Boolean lineageRequired;
    private List<String> languages;
    private JsonNode languageOptions;
    private List<String> proficiencies;
    private List<String> resistances;
    private List<String> vulnerabilities;
    private List<String> immunities;
    private List<String> conditionResistances;
    private List<String> conditionAdvantages;
    private JsonNode innateSpells;
    private Boolean allowAbilityScoreBonuses;

    @Valid
    private List<RaceAbilityScoreBonusDto> abilityScoreBonuses;

    private JsonNode metadata;
}
