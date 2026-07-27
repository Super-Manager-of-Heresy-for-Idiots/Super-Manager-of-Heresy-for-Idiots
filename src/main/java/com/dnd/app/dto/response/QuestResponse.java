package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс QuestResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestResponse {
    private UUID id;
    private String title;
    private String description;
    private String status;
    private Boolean isVisibleToPlayers;
    /** Сдача квеста квестодателю сразу выдаёт награду (true) или ждёт подтверждения ГМа (false). */
    private Boolean autoCompleteOnTurnIn;
    /** Арт квеста: прокси-URL media-ассета или null. */
    private String artUrl;
    private List<NoteResponse> notes;
    private List<?> rewards;
    /** NPC-квестодатели, у которых игрок может взять и сдать этот квест. Заполняется только в детальном ответе. */
    private List<NpcRef> linkedNpcs;
    private Instant createdAt;
    private Instant updatedAt;

    /** Короткая ссылка на NPC-квестодателя. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NpcRef {
        private UUID id;
        private String name;
    }
}
