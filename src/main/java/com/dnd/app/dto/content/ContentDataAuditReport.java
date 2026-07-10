package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс ContentDataAuditReport описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ContentDataAuditReport", description = "Completeness report for the new content model")
public class ContentDataAuditReport {

    @Schema(description = "Number of core (non-homebrew) classes", example = "14")
    private int coreClassCount;

    @Schema(description = "Per-class completeness rows")
    private List<ClassAuditEntry> classes;

    @Schema(description = "Slugs of core classes missing mechanics (hit die / primary abilities)")
    private List<String> classesMissingMechanics;

    @Schema(description = "Slugs of core classes with no class features seeded")
    private List<String> classesWithoutFeatures;

    @Schema(description = "Slugs of core classes with no reward groups")
    private List<String> classesWithoutRewardGroups;

    @Schema(description = "Slugs of core classes with no subclass-choice reward group")
    private List<String> classesWithoutSubclassChoice;

    @Schema(description = "Ids of CHOICE reward groups that have no options")
    private List<UUID> choiceGroupsWithoutOptions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "ClassAuditEntry", description = "Per-class completeness row")
    public static class ClassAuditEntry {
        private UUID classId;
        private String slug;
        private String name;
        private boolean hasMechanics;
        private int featureCount;
        private int rewardGroupCount;
        private int subclassCount;
        private boolean hasSubclassChoiceGroup;
    }
}
