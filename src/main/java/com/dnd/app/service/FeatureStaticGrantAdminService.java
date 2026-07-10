package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureLanguageGrant;
import com.dnd.app.domain.featurerule.FeatureProficiencyGrant;
import com.dnd.app.domain.featurerule.FeatureProficiencyType;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.GrantTiming;
import com.dnd.app.dto.featurerule.StaticGrantAdminResponse;
import com.dnd.app.dto.featurerule.StaticGrantEditRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureLanguageGrantRepository;
import com.dnd.app.repository.FeatureProficiencyGrantRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureStaticGrantAdminService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureStaticGrantAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureProficiencyGrantRepository proficiencyRepository;
    private final FeatureLanguageGrantRepository languageRepository;

    /**
     * Возвращает результат операции "get" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public StaticGrantAdminResponse get(UUID ruleId) {
        requireRule(ruleId);
        return toResponse(ruleId);
    }

    /**
     * Выполняет операции "replace" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param req входящее значение req, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public StaticGrantAdminResponse replace(UUID ruleId, StaticGrantEditRequest req) {
        FeatureRule rule = requireRule(ruleId);
        proficiencyRepository.deleteByFeatureRuleId(rule.getId());
        languageRepository.deleteByFeatureRuleId(rule.getId());

        for (StaticGrantEditRequest.ProficiencyGrant row : safe(req.getProficiencyGrants())) {
            String type = required(row.getProficiencyType(), "Тип владения обязателен");
            if (FeatureProficiencyType.fromCode(type).isEmpty()) {
                throw new BadRequestException("Неизвестный тип владения: " + type);
            }
            proficiencyRepository.save(FeatureProficiencyGrant.builder()
                    .featureRuleId(rule.getId())
                    .proficiencyType(type)
                    .targetId(row.getTargetId())
                    .filterRuleId(row.getFilterRuleId())
                    .expertise(row.isExpertise())
                    .grantTiming(timing(row.getGrantTiming()))
                    .build());
        }
        for (StaticGrantEditRequest.LanguageGrant row : safe(req.getLanguageGrants())) {
            languageRepository.save(FeatureLanguageGrant.builder()
                    .featureRuleId(rule.getId())
                    .languageId(row.getLanguageId())
                    .filterRuleId(row.getFilterRuleId())
                    .grantTiming(timing(row.getGrantTiming()))
                    .build());
        }
        return toResponse(rule.getId());
    }

    private FeatureRule requireRule(UUID ruleId) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        if (!FeatureRuleProfile.STATIC_GRANT.getCode().equals(rule.getRuleType())) {
            throw new BadRequestException("Редактор static grants доступен только для static_grant правил");
        }
        return rule;
    }

    private StaticGrantAdminResponse toResponse(UUID ruleId) {
        List<StaticGrantAdminResponse.ProficiencyGrant> profs = proficiencyRepository.findByFeatureRuleId(ruleId)
                .stream()
                .sorted(Comparator.comparing(FeatureProficiencyGrant::getId))
                .map(g -> StaticGrantAdminResponse.ProficiencyGrant.builder()
                        .id(g.getId())
                        .proficiencyType(g.getProficiencyType())
                        .targetId(g.getTargetId())
                        .filterRuleId(g.getFilterRuleId())
                        .expertise(g.isExpertise())
                        .grantTiming(g.getGrantTiming())
                        .build())
                .toList();
        List<StaticGrantAdminResponse.LanguageGrant> langs = languageRepository.findByFeatureRuleId(ruleId)
                .stream()
                .sorted(Comparator.comparing(FeatureLanguageGrant::getId))
                .map(g -> StaticGrantAdminResponse.LanguageGrant.builder()
                        .id(g.getId())
                        .languageId(g.getLanguageId())
                        .filterRuleId(g.getFilterRuleId())
                        .grantTiming(g.getGrantTiming())
                        .build())
                .toList();
        return StaticGrantAdminResponse.builder().proficiencyGrants(profs).languageGrants(langs).build();
    }

    private static String timing(String raw) {
        String code = raw == null || raw.isBlank() ? GrantTiming.LEVEL_UP.getCode() : raw.trim();
        if (GrantTiming.fromCode(code).isEmpty()) {
            throw new BadRequestException("Неизвестное время выдачи: " + code);
        }
        return code;
    }

    private static String required(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(message);
        }
        return raw.trim();
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
