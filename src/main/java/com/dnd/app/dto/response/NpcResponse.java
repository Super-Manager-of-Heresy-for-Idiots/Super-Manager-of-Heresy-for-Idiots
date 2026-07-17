package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.NpcRole;
import com.dnd.app.domain.enums.NpcSourceType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс NpcResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NpcResponse {
    private UUID id;
    private String name;
    private String publicDescription;
    private String privateDescription;
    private Boolean isVisibleToPlayers;
    /** Портрет NPC: прокси-URL media-ассета (/api/media/{assetId}/content) или null, если не загружен. */
    private String portraitUrl;

    private NpcSourceType sourceType;
    private NpcRole npcRole;

    // CLASS_BASED
    private Ref race;
    private Ref characterClass;
    private Integer level;
    private String abilities;
    private List<Ref> spells;

    // MONSTER_BASED
    private Ref sourceMonster;

    private List<NoteResponse> notes;
    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ref {
        private UUID id;
        private String name;
    }
}
