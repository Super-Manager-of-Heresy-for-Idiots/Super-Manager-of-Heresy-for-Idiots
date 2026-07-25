package com.dnd.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс CreateBattleRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBattleRequest {

    @Size(max = 120, message = "Battle name must be at most 120 characters")
    private String name;

    /** Локация, в которой происходит бой (WORLD_PLAN Этап 4). Опционально. */
    private java.util.UUID locationId;
}
