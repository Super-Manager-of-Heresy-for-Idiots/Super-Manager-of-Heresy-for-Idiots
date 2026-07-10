package com.dnd.app.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс UpdateCharacterRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCharacterRequest {

    @Size(max = 100, message = "Имя персонажа не должно превышать 100 символов")
    private String name;

    @Size(max = 100, message = "Имя игрока не должно превышать 100 символов")
    private String playerName;

    private String proficiencies;

    private String equipment;

    private String features;

    @Size(max = 40, message = "Мировоззрение не должно превышать 40 символов")
    private String alignment;

    private BiographyEntry biography;

    private java.util.List<AttackEntry> attacks;

    private UUID raceId;

    private UUID selectedLineageId;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BiographyEntry {
        private String personalityTraits;
        private String ideals;
        private String bonds;
        private String flaws;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttackEntry {
        private String name;
        private String attackBonus;
        private String damage;
        private String damageType;
    }
}
