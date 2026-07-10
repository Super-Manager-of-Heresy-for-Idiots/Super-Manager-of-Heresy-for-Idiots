package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс FeatureRuleIssueResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleIssueResponse {
    private UUID id;
    private String ownerType;
    private UUID ownerId;
    private UUID featureRuleId;
    private String issueType;
    private String severity;
    private String message;
    private String sourceTextFragment;
    private boolean resolved;
    private UUID resolvedBy;
    private Instant resolvedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Feature context (for the global issue list / cross-feature views).
    private String featureTitle;
    private String className;
    private String subclassName;
    private Integer level;
}
