package com.dnd.app.dto.request;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
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
public class RaceLineageRequest {

    private UUID id;

    @NotBlank(message = "Lineage name is required")
    @Size(max = 100, message = "Lineage name must be <= 100 characters")
    private String name;

    private String description;

    @Valid
    private List<RaceTraitRequest> traits;

    private JsonNode innateSpells;
    private List<String> resistances;
    private RaceSpeedDto speedModifiers;
    private JsonNode metadata;
}
