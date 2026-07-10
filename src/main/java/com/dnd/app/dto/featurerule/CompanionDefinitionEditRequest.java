package com.dnd.app.dto.featurerule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс CompanionDefinitionEditRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanionDefinitionEditRequest {
    @Valid
    @Size(max = 20)
    private List<Companion> companions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Companion {
        @Size(max = 64)
        private String companionKey;
        private UUID monsterId;
        @Size(max = 120)
        private String nameTemplate;
        @Size(max = 2000)
        private String hpFormula;
        @Size(max = 2000)
        private String acFormula;
        @Size(max = 2000)
        private String attackBonusFormula;
        @Size(max = 32)
        private String summonTiming;
        private Integer sortOrder;
        @Size(max = 4000)
        private String notes;
    }
}
