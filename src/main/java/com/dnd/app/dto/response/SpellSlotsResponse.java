package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс SpellSlotsResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellSlotsResponse {

    private List<SlotLevel> levels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotLevel {
        private int spellLevel;
        private int max;
        private int expended;
        private int available;
    }
}
