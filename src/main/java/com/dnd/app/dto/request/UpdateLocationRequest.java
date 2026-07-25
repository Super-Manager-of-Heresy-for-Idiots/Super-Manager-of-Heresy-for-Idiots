package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.LocationRestSafety;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс UpdateLocationRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLocationRequest {

    @Size(max = 100, message = "Location name must not exceed 100 characters")
    private String name;

    private String description;

    private Boolean isVisibleToPlayers;

    /** Метка безопасности привала (CAMP): SAFE | RISKY | DANGEROUS. */
    private LocationRestSafety restSafety;
}
