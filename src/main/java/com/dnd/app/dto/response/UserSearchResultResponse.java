package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.RelationshipView;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс UserSearchResultResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchResultResponse {
    private UUID id;
    private String username;
    private String role;
    private RelationshipView relationship;
}
