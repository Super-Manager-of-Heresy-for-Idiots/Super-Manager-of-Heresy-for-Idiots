package com.dnd.app.dto.response;

import com.dnd.app.dto.request.RaceAbilityScoreBonusDto;
import com.dnd.app.dto.request.RaceLineageRequest;
import com.dnd.app.dto.request.RaceSpeedDto;
import com.dnd.app.dto.request.RaceTraitRequest;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceResponse {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private String loreDescription;
    private String sourceType;
    private String sourceName;
    private Boolean active;
    private UUID createdBy;
    private String createdByUsername;
    private UUID updatedBy;
    private String updatedByUsername;
    private UUID homebrewId;
    private String homebrewTitle;
    private String creatureType;
    private List<String> sizeOptions;
    private String defaultSize;
    private RaceSpeedDto speed;
    private Integer darkvisionRange;
    private List<RaceTraitRequest> traits;
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
    private List<RaceAbilityScoreBonusDto> abilityScoreBonuses;
    private JsonNode metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
