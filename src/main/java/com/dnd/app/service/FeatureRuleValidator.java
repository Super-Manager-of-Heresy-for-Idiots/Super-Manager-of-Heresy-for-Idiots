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
 * Класс FeatureRuleValidator описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class FeatureRuleValidator {

    private static final String ERROR = FeatureIssueSeverity.ERROR.getCode();

    private final FeatureRuleIssueRepository issueRepository;

    /**
     * Проверяет корректность операции "validate" в рамках бизнес-логики домена.
     * @param rule входящее значение rule, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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
