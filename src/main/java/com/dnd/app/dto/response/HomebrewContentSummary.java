package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс HomebrewContentSummary описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomebrewContentSummary {

    @Builder.Default
    private int itemTypeCount = 0;
    @Builder.Default
    private int classCount = 0;
    @Builder.Default
    private int raceCount = 0;
    @Builder.Default
    private int skillCount = 0;
    @Builder.Default
    private int featCount = 0;
    @Builder.Default
    private int subclassCount = 0;
    @Builder.Default
    private int buffDebuffCount = 0;
}
