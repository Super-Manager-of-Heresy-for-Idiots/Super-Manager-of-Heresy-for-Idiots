package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс ContentSeedSummary описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ContentSeedSummary", description = "Result of an idempotent content backfill run")
public class ContentSeedSummary {

    @Schema(description = "Number of classes for which a group was created this run", example = "14")
    private int created;

    @Schema(description = "Number of classes skipped (already seeded or no data)", example = "0")
    private int skipped;

    @Schema(description = "Slugs of classes seeded this run")
    private List<String> createdClassSlugs;

    @Schema(description = "Slugs skipped because they were already seeded")
    private List<String> alreadyPresentClassSlugs;
}
