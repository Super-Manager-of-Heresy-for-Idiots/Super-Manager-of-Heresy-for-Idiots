package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.dto.featurerule.FeatureRuleValidationResponse;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared lightweight rule validation used by both the admin service and the revision service (kept as
 * a separate component to avoid a service dependency cycle).
 *
 * <p>Stage 1/2 scope: known rule type and no unresolved error issue. Later stages extend this with
 * formula/reference/profile-specific checks.</p>
 */
@Component
@RequiredArgsConstructor
public class FeatureRuleValidator {

    private static final String ERROR = FeatureIssueSeverity.ERROR.getCode();

    private final FeatureRuleIssueRepository issueRepository;

    public FeatureRuleValidationResponse validate(FeatureRule rule) {
        List<String> problems = new ArrayList<>();
        if (FeatureRuleProfile.fromCode(rule.getRuleType()).isEmpty()) {
            problems.add("Неизвестный тип правила: " + rule.getRuleType());
        }
        if (issueRepository.existsByFeatureRuleIdAndResolvedFalseAndSeverity(rule.getId(), ERROR)) {
            problems.add("Есть неразрешённая ошибка (error), связанная с правилом");
        }
        return FeatureRuleValidationResponse.builder()
                .valid(problems.isEmpty())
                .problems(problems)
                .build();
    }
}
