package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.ItemInstanceFeatureResource;
import com.dnd.app.domain.featurerule.RestType;
import com.dnd.app.dto.featurerule.RestResourcePreview;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.ItemInstanceFeatureResourceRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.repository.RestTypeRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс RestFeatureRuntimeService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RestFeatureRuntimeService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureResourceDefinitionRepository definitionRepository;
    private final RestTypeRepository restTypeRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;
    private final EffectExpirationService effectExpirationService;
    private final ItemInstanceFeatureResourceRepository itemResourceRepository;
    private final ItemInstanceRepository itemInstanceRepository;

    /**
     * Выполняет операции "preview" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param restTypeCode входящее значение rest type code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<RestResourcePreview> preview(PlayerCharacter character, String restTypeCode) {
        List<RestResourcePreview> out = new ArrayList<>(compute(character, restTypeCode, false));
        out.addAll(computeItems(character, restTypeCode, false));
        return out;
    }

    /**
     * Выполняет операции "complete" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param restTypeCode входящее значение rest type code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public List<RestResourcePreview> complete(PlayerCharacter character, String restTypeCode) {
        List<RestResourcePreview> restored = new ArrayList<>(compute(character, restTypeCode, true));
        // ITEM_ABIL Фаза 3 §4.4: восстановление зарядов предметов (item_instance_feature_resource) на отдыхе.
        restored.addAll(computeItems(character, restTypeCode, true));
        // Stage 7: end effects that expire on this rest (gated: no-op if no effect data / flag off).
        effectExpirationService.endOnRest(character, restTypeCode);
        return restored;
    }

    private List<RestResourcePreview> compute(PlayerCharacter character, String restTypeCode, boolean apply) {
        if (!flags.resourcesActive()) {
            return List.of();
        }
        RestType restType = restTypeRepository.findByCode(restTypeCode).orElse(null);
        if (restType == null) {
            return List.of();
        }
        List<CharacterFeatureResource> resources = resourceRepository.findByCharacterId(character.getId());
        if (resources.isEmpty()) {
            return List.of();
        }
        List<UUID> defIds = resources.stream()
                .map(CharacterFeatureResource::getResourceDefinitionId).distinct().toList();
        Map<UUID, FeatureResourceDefinition> defs = definitionRepository.findAllById(defIds).stream()
                .collect(Collectors.toMap(FeatureResourceDefinition::getId, d -> d));

        FormulaContext ctx = contextFactory.build(character);
        List<RestResourcePreview> out = new ArrayList<>();
        for (CharacterFeatureResource res : resources) {
            FeatureResourceDefinition def = defs.get(res.getResourceDefinitionId());
            if (def == null || !restType.getId().equals(def.getResetRestTypeId())) {
                continue;
            }
            Integer target = resetTarget(res, def, ctx);
            if (target == null) {
                continue;
            }
            out.add(RestResourcePreview.builder()
                    .resourceId(res.getId())
                    .resourceKey(def.getResourceKey())
                    .displayName(def.getDisplayName())
                    .currentValue(res.getCurrentValue())
                    .willBeValue(target)
                    .build());
            if (apply) {
                res.setCurrentValue(target);
                res.setLastResetAt(Instant.now());
                resourceRepository.save(res);
            }
        }
        return out;
    }

    /**
     * Восстанавливает заряды предметов персонажа на отдыхе (scope=ITEM_INSTANCE, ITEM_ABIL Фаза 3 §4.4).
     * Гейт — флаг items-enabled; семантика сброса та же, что у CHARACTER-ресурсов (resetRestTypeId + resetAmountFormulaId).
     * @param character персонаж-владелец предметов
     * @param restTypeCode код типа отдыха
     * @param apply true — применить восстановление, false — только предпросмотр
     * @return список превью восстановленных зарядов предметов
     */
    private List<RestResourcePreview> computeItems(PlayerCharacter character, String restTypeCode, boolean apply) {
        if (!flags.itemsActive()) {
            return List.of();
        }
        RestType restType = restTypeRepository.findByCode(restTypeCode).orElse(null);
        if (restType == null) {
            return List.of();
        }
        List<UUID> instanceIds = itemInstanceRepository.findByOwnerCharacterId(character.getId()).stream()
                .map(ItemInstance::getId).toList();
        if (instanceIds.isEmpty()) {
            return List.of();
        }
        List<ItemInstanceFeatureResource> resources = itemResourceRepository.findByItemInstanceIdIn(instanceIds);
        if (resources.isEmpty()) {
            return List.of();
        }
        List<UUID> defIds = resources.stream()
                .map(ItemInstanceFeatureResource::getResourceDefinitionId).distinct().toList();
        Map<UUID, FeatureResourceDefinition> defs = definitionRepository.findAllById(defIds).stream()
                .collect(Collectors.toMap(FeatureResourceDefinition::getId, d -> d));

        FormulaContext ctx = contextFactory.build(character);
        List<RestResourcePreview> out = new ArrayList<>();
        for (ItemInstanceFeatureResource res : resources) {
            FeatureResourceDefinition def = defs.get(res.getResourceDefinitionId());
            if (def == null || !restType.getId().equals(def.getResetRestTypeId())) {
                continue;
            }
            Integer target = resetTargetFor(res.getCurrentValue(), res.getMaxValueSnapshot(), def, ctx, res.getId());
            if (target == null) {
                continue;
            }
            out.add(RestResourcePreview.builder()
                    .resourceId(res.getId())
                    .resourceKey(def.getResourceKey())
                    .displayName(def.getDisplayName())
                    .currentValue(res.getCurrentValue())
                    .willBeValue(target)
                    .build());
            if (apply) {
                res.setCurrentValue(target);
                res.setUpdatedAt(Instant.now());
                itemResourceRepository.save(res);
            }
        }
        return out;
    }

    /** Target value after reset: current + reset_amount (capped at max), or full restore to max. */
    private Integer resetTarget(CharacterFeatureResource res, FeatureResourceDefinition def, FormulaContext ctx) {
        return resetTargetFor(res.getCurrentValue(), res.getMaxValueSnapshot(), def, ctx, res.getId());
    }

    /**
     * Общая логика целевого значения после сброса (для CHARACTER- и ITEM_INSTANCE-ресурсов):
     * current + reset_amount (не выше max), либо полное восстановление до max.
     * @param currentValue текущее значение зарядов
     * @param maxSnapshot зафиксированный максимум
     * @param def определение ресурса
     * @param ctx контекст формул персонажа
     * @param resourceId id ресурса (для логов)
     * @return целевое значение или null, если максимум неизвестен (вызывающий пропускает)
     */
    private Integer resetTargetFor(Integer currentValue, Integer maxSnapshot, FeatureResourceDefinition def,
                                   FormulaContext ctx, UUID resourceId) {
        if (def.getResetAmountFormulaId() != null) {
            FeatureFormula formula = formulaRepository.findById(def.getResetAmountFormulaId()).orElse(null);
            if (formula != null) {
                try {
                    int amount = formulaService.evaluateInteger(formula, ctx);
                    int current = currentValue != null ? currentValue : 0;
                    int target = current + amount;
                    return maxSnapshot != null ? Math.min(target, maxSnapshot) : target;
                } catch (FormulaException e) {
                    log.warn("Reset amount formula failed for resource {}: {}", resourceId, e.getMessage());
                }
            }
        }
        return maxSnapshot; // full restore (null if max unknown → caller skips)
    }
}
