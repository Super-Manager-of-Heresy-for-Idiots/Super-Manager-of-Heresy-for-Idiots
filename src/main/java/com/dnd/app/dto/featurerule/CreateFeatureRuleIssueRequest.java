package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Raise an issue on a feature (feature-level) or on a specific rule ({@code featureRuleId} set). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeatureRuleIssueRequest {

    /** Optional; when set, the issue is attached to that specific rule. */
    private UUID featureRuleId;

    /** {@code rule_issue_type} code, e.g. {@code ambiguous_parse}. */
    @NotBlank(message = "Тип проблемы обязателен")
    @Size(max = 48)
    private String issueType;

    /** {@code FeatureIssueSeverity} code: info / warn / error. */
    @NotBlank(message = "Severity обязателен")
    @Size(max = 16)
    private String severity;

    @NotBlank(message = "Сообщение обязательно")
    @Size(max = 4000)
    private String message;

    @Size(max = 4000)
    private String sourceTextFragment;
}
