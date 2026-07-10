package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureTrigger;
import com.dnd.app.domain.featurerule.GameplayEvent;
import com.dnd.app.domain.featurerule.PendingGameplayPrompt;
import com.dnd.app.domain.featurerule.TriggerEventType;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureTriggerRepository;
import com.dnd.app.repository.GameplayEventRepository;
import com.dnd.app.repository.PendingGameplayPromptRepository;
import com.dnd.app.repository.TriggerEventTypeRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Класс GameplayEventService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameplayEventService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final TriggerEventTypeRepository eventTypeRepository;
    private final FeatureTriggerRepository triggerRepository;
    private final GameplayEventRepository eventRepository;
    private final PendingGameplayPromptRepository promptRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final FeatureResourceService featureResourceService;

    /**
     * Публикует событие операции "publish" в рамках бизнес-логики домена.
     * @param actor входящее значение actor, используемое бизнес-сценарием
     * @param eventType входящее значение event type, используемое бизнес-сценарием
     * @param combatId идентификатор combat, используемый для выбора нужного бизнес-объекта
     * @param payloadJson входящее значение payload json, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int publish(PlayerCharacter actor, String eventType, UUID combatId, String payloadJson) {
        if (!flags.triggersActive()) {
            return 0;
        }
        GameplayEvent event = eventRepository.save(GameplayEvent.builder()
                .eventType(eventType).combatId(combatId).actorCharacterId(actor.getId())
                .payloadJson(payloadJson).build());

        TriggerEventType eventTypeRow = eventTypeRepository.findByCode(eventType).orElse(null);
        if (eventTypeRow == null) {
            return 0;
        }
        Map<UUID, ClassFeature> featureById = resolver.knownBaseClassFeatures(actor.getId()).stream()
                .collect(Collectors.toMap(ClassFeature::getId, Function.identity(), (a, b) -> a));
        List<FeatureRule> rules = resolver.approvedEnabledRules(featureById.keySet());
        if (rules.isEmpty()) {
            return 0;
        }
        Map<UUID, FeatureRule> ruleById = rules.stream()
                .collect(Collectors.toMap(FeatureRule::getId, Function.identity(), (a, b) -> a));

        List<FeatureTrigger> triggers = triggerRepository.findByFeatureRuleIdIn(ruleById.keySet()).stream()
                .filter(t -> eventTypeRow.getId().equals(t.getEventTypeId()))
                .toList();
        if (triggers.isEmpty()) {
            return 0;
        }
        FormulaContext ctx = contextFactory.build(actor);

        int promptsCreated = 0;
        for (FeatureTrigger trigger : triggers) {
            if (!predicatePasses(trigger, ctx)) {
                continue;
            }
            FeatureRule rule = ruleById.get(trigger.getFeatureRuleId());
            UUID featureId = rule != null ? rule.getOwnerId() : null;

            if (trigger.isRequiresPlayerConfirmation()) {
                promptRepository.save(PendingGameplayPrompt.builder()
                        .combatId(combatId)
                        .characterId(actor.getId())
                        .sourceFeatureId(featureId)
                        .triggerEventId(event.getId())
                        .featureTriggerId(trigger.getId())
                        .promptType("reaction")
                        .status("pending")
                        .build());
                promptsCreated++;
            } else {
                spendResource(actor.getId(), trigger.getConsumesResourceDefinitionId());
            }
        }
        return promptsCreated;
    }

    private boolean predicatePasses(FeatureTrigger trigger, FormulaContext ctx) {
        if (trigger.getPredicateFormulaId() == null) {
            return true;
        }
        FeatureFormula formula = formulaRepository.findById(trigger.getPredicateFormulaId()).orElse(null);
        if (formula == null) {
            return true;
        }
        try {
            return formulaService.evaluateBoolean(formula, ctx);
        } catch (FormulaException e) {
            log.warn("Trigger predicate failed for {}: {}", trigger.getId(), e.getMessage());
            return false;
        }
    }

    void spendResource(UUID characterId, UUID resourceDefinitionId) {
        if (resourceDefinitionId == null) {
            return;
        }
        FeatureResourceDefinition def = resourceDefinitionRepository.findById(resourceDefinitionId).orElse(null);
        if (def == null) {
            return;
        }
        CharacterFeatureResource res = def.getSharedPoolKey() != null && !def.getSharedPoolKey().isBlank()
                ? resourceRepository.findFirstByCharacterIdAndSharedPoolKey(characterId, def.getSharedPoolKey()).orElse(null)
                : resourceRepository.findByCharacterIdAndResourceDefinitionId(characterId, def.getId()).orElse(null);
        if (res != null) {
            try {
                featureResourceService.spend(characterId, res.getId(), 1);
            } catch (RuntimeException e) {
                log.warn("Trigger resource spend failed for character {}: {}", characterId, e.getMessage());
            }
        }
    }
}
