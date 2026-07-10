package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс CharacterRewardsResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterRewardsResponse {
    private UUID characterId;
    private Integer totalLevel;
    private List<ClassBreakdown> classBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassBreakdown {
        private UUID classId;
        private String className;
        private Integer classLevel;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private SubclassInfo subclass;

        private Map<String, List<AcquiredReward>> rewardsByType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubclassInfo {
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcquiredReward {
        private String name;
        private Instant acquiredAt;
    }
}
