package com.dnd.app.service;

import com.dnd.app.domain.featurerule.*;
import com.dnd.app.dto.featurerule.ChoiceRuleAdminResponse;
import com.dnd.app.dto.featurerule.ChoiceRuleEditRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureChoiceGroupRepository;
import com.dnd.app.repository.FeatureChoiceOptionRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureChoiceRuleAdminService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureChoiceRuleAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureChoiceGroupRepository groupRepository;
    private final FeatureChoiceOptionRepository optionRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    /**
     * Возвращает результат операции "get" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ChoiceRuleAdminResponse get(UUID ruleId) {
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
    public ChoiceRuleAdminResponse replace(UUID ruleId, ChoiceRuleEditRequest req) {
        FeatureRule rule = requireRule(ruleId);
        List<UUID> groupIds = groupRepository.findByFeatureRuleId(rule.getId()).stream()
                .map(FeatureChoiceGroup::getId).toList();
        if (!groupIds.isEmpty()) {
            optionRepository.deleteByChoiceGroupIdIn(groupIds);
        }
        groupRepository.deleteByFeatureRuleId(rule.getId());

        for (ChoiceRuleEditRequest.Group row : safe(req.getGroups())) {
            String key = required(row.getChoiceKey(), "Ключ выбора обязателен");
            int min = row.getMinChoices() == null ? 1 : row.getMinChoices();
            if (min < 0) {
                throw new BadRequestException("Минимум выборов не может быть отрицательным");
            }
            FeatureChoiceGroup group = groupRepository.save(FeatureChoiceGroup.builder()
                    .featureRuleId(rule.getId())
                    .choiceKey(key)
                    .minChoices(min)
                    .maxChoicesFormulaId(formulaHelper.upsert(null, row.getMaxChoicesFormula(),
                            FormulaResultType.INTEGER.getCode()))
                    .choiceTiming(choiceTiming(row.getChoiceTiming()))
                    .replacePolicy(replacePolicy(row.getReplacePolicy()))
                    .build());
            int optionOrder = 0;
            for (ChoiceRuleEditRequest.Option option : safe(row.getOptions())) {
                String type = required(option.getOptionType(), "Тип опции обязателен");
                if (ChoiceOptionType.fromCode(type).isEmpty()) {
                    throw new BadRequestException("Неизвестный тип опции выбора: " + type);
                }
                optionRepository.save(FeatureChoiceOption.builder()
                        .choiceGroupId(group.getId())
                        .optionType(type)
                        .targetEntityId(option.getTargetEntityId())
                        .filterRuleId(option.getFilterRuleId())
                        .sortOrder(option.getSortOrder() != null ? option.getSortOrder() : optionOrder)
                        .build());
                optionOrder++;
            }
        }
        return toResponse(rule.getId());
    }

    private FeatureRule requireRule(UUID ruleId) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        if (!FeatureRuleProfile.CHOICE.getCode().equals(rule.getRuleType())) {
            throw new BadRequestException("Редактор choices доступен только для choice правил");
        }
        return rule;
    }

    private ChoiceRuleAdminResponse toResponse(UUID ruleId) {
        List<ChoiceRuleAdminResponse.Group> groups = groupRepository.findByFeatureRuleId(ruleId).stream()
                .sorted(Comparator.comparing(FeatureChoiceGroup::getChoiceKey))
                .map(group -> {
                    FeatureFormula max = formulaHelper.find(group.getMaxChoicesFormulaId());
                    List<ChoiceRuleAdminResponse.Option> options = optionRepository
                            .findByChoiceGroupIdOrderBySortOrderAsc(group.getId()).stream()
                            .map(option -> ChoiceRuleAdminResponse.Option.builder()
                                    .id(option.getId())
                                    .optionType(option.getOptionType())
                                    .targetEntityId(option.getTargetEntityId())
                                    .filterRuleId(option.getFilterRuleId())
                                    .sortOrder(option.getSortOrder())
                                    .build())
                            .toList();
                    return ChoiceRuleAdminResponse.Group.builder()
                            .id(group.getId())
                            .choiceKey(group.getChoiceKey())
                            .minChoices(group.getMinChoices())
                            .maxChoicesFormula(max != null ? max.getExpression() : null)
                            .maxChoicesFormulaStatus(max != null ? max.getValidationStatus() : null)
                            .maxChoicesFormulaMessage(max != null ? max.getValidationMessage() : null)
                            .choiceTiming(group.getChoiceTiming())
                            .replacePolicy(group.getReplacePolicy())
                            .options(options)
                            .build();
                })
                .toList();
        return ChoiceRuleAdminResponse.builder().groups(groups).build();
    }

    private static String choiceTiming(String raw) {
        String code = raw == null || raw.isBlank() ? ChoiceTiming.LEVEL_UP.getCode() : raw.trim();
        if (ChoiceTiming.fromCode(code).isEmpty()) {
            throw new BadRequestException("Неизвестное время выбора: " + code);
        }
        return code;
    }

    private static String replacePolicy(String raw) {
        String code = raw == null || raw.isBlank() ? ChoiceReplacePolicy.NEVER.getCode() : raw.trim();
        if (ChoiceReplacePolicy.fromCode(code).isEmpty()) {
            throw new BadRequestException("Неизвестная политика замены выбора: " + code);
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
