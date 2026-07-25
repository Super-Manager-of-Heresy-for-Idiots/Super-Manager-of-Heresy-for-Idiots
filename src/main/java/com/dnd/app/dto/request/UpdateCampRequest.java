package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс UpdateCampRequest описывает правку привала мастером: название, описание, день,
 * локация и состав. Незаданные поля не изменяются; состав меняется только если список передан.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampRequest {

    @Size(max = 120, message = "Camp name must not exceed 120 characters")
    private String name;

    private String description;

    @Min(value = 1, message = "Day number must be positive")
    private Integer dayNumber;

    private UUID locationId;

    /** Явный сброс локации: привал становится "в пути". */
    private Boolean clearLocation;

    /** Новый состав привала; null — состав не трогаем. */
    private List<UUID> participantCharacterIds;
}
