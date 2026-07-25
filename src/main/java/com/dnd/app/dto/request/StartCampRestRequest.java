package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс StartCampRestRequest описывает объявление привала мастером: тип отдыха.
 * Принимаются как сокращения API ({@code long} / {@code short}), так и канонические коды.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StartCampRestRequest {

    @NotBlank(message = "Rest type is required")
    private String restType;
}
