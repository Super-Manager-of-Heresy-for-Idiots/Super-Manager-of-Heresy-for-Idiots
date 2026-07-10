package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс RuntimeMigrationReport описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "RuntimeMigrationReport", description = "Legacy→new runtime ID migration report")
public class RuntimeMigrationReport {

    @Schema(description = "True => no writes performed (preview only)")
    private boolean dryRun;

    @Schema(description = "One migration block per runtime FK column")
    private List<EntityMigration> entities;

    @Schema(description = "Operational notes (post-validation, backup requirement, etc.)")
    private List<String> notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "EntityMigration")
    public static class EntityMigration {
        @Schema(description = "What is migrated", example = "character_class_levels.class_id -> character_class")
        private String target;
        @Schema(description = "Distinct ids already pointing at the new model")
        private int alreadyNew;
        @Schema(description = "Unique name matches (safe to apply)")
        private List<Mapping> mapped;
        @Schema(description = "Multiple candidates — need manual review, never auto-applied")
        private List<Mapping> ambiguous;
        @Schema(description = "No candidate found — need manual review")
        private List<Mapping> unmapped;
        @Schema(description = "Rows updated this run (0 in dry-run)")
        private int rowsUpdated;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "MigrationMapping")
    public static class Mapping {
        private UUID legacyId;
        private String legacyName;
        private UUID newId;
        private String newName;
        @Schema(description = "Number of new-model candidates matched by name")
        private int candidateCount;
    }
}
