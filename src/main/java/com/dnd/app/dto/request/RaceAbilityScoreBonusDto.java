package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RaceAbilityScoreBonusDto {

    @NotBlank(message = "Ability is required")
    private String ability;

    @NotNull(message = "Ability bonus value is required")
    private Integer value;

    @NotBlank(message = "Ability bonus mode is required")
    private String mode;

    private String description;
}
