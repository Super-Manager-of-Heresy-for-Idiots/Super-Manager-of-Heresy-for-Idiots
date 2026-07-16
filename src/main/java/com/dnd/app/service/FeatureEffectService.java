package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.EffectStackingPolicy;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectEndCondition;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.repository.DurationUnitRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectEndConditionRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureEffectService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureEffectService {

    private static final String ACTIVE = ActiveEffectStatus.ACTIVE.getCode();
    private static final String ENDED = ActiveEffectStatus.ENDED.getCode();

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final FeatureEffectDefinitionRepository definitionRepository;
    private final FeatureEffectEndConditionRepository endConditionRepository;
    private final FeatureActiveEffectRepository activeRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final DurationUnitRepository durationUnitRepository;
    private final CharacterFormulaContextFactory contextFactory;
    private final ActiveEffectConditionLinker conditionLinker;

    /**
     * Выполняет операции "apply for feature use" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int applyForFeatureUse(PlayerCharacter character, UUID featureId) {
        if (!flags.effectsActive()) {
            return 0; // gate before any resolution work: disabled must stay a strict no-op
        }
        List<FeatureRule> rules = resolver.approvedEnabledRules(List.of(featureId));
        return applyRules(character, character, featureId, null, rules);
    }

    @Transactional
    public int applyForItemRuleUse(PlayerCharacter character, UUID itemInstanceId, FeatureRule rule) {
        if (!flags.effectsActive()) {
            return 0;
        }
        if (rule == null) {
            return 0;
        }
        return applyRules(character, character, null, itemInstanceId, List.of(rule));
    }

    /**
     * Выполняет операции "apply for spell cast" в рамках бизнес-логики домена.
     * @param caster входящее значение caster, используемое бизнес-сценарием
     * @param target входящее значение target, используемое бизнес-сценарием
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int applyForSpellCast(PlayerCharacter caster, PlayerCharacter target, UUID spellId) {
        if (!flags.effectsActive()) {
            return 0;
        }
        List<FeatureRule> rules =
                resolver.approvedEnabledRules(FeatureRuleOwnerType.SPELL, List.of(spellId));
        // Concentration (2.2): starting a new concentration spell ends the caster's previous one
        // (a creature concentrates on only one spell at a time). Done BEFORE applying the new effects
        // so the fresh ones survive.
        if (requiresConcentration(rules)) {
            endConcentration(caster.getId());
        }
        return applyRules(target, caster, null, null, rules);
    }

    /** Whether any of the rules' effect definitions require concentration. */
    private boolean requiresConcentration(List<FeatureRule> rules) {
        if (rules.isEmpty()) {
            return false;
        }
        List<UUID> ruleIds = rules.stream().map(FeatureRule::getId).toList();
        return definitionRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .anyMatch(FeatureEffectDefinition::isConcentrationRequired);
    }

    /**
     * Проверяет условие операции "is concentrating" в рамках бизнес-логики домена.
     * @param casterId идентификатор caster, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public boolean isConcentrating(UUID casterId) {
        return !concentrationEffects(casterId).isEmpty();
    }

    /**
     * Выполняет операции "end concentration" в рамках бизнес-логики домена.
     * @param casterId идентификатор caster, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int endConcentration(UUID casterId) {
        List<FeatureActiveEffect> concentration = concentrationEffects(casterId);
        for (FeatureActiveEffect effect : concentration) {
            conditionLinker.clear(effect);
            effect.setStatus(ENDED);
            activeRepository.save(effect);
        }
        return concentration.size();
    }

    /** The caster's ACTIVE effects whose definition requires concentration (keyed by {@code sourceCharacterId}). */
    private List<FeatureActiveEffect> concentrationEffects(UUID casterId) {
        List<FeatureActiveEffect> active = activeRepository.findBySourceCharacterIdAndStatus(casterId, ACTIVE);
        if (active.isEmpty()) {
            return List.of();
        }
        return active.stream()
                .filter(e -> definitionRepository.findById(e.getEffectDefinitionId())
                        .map(FeatureEffectDefinition::isConcentrationRequired).orElse(false))
                .toList();
    }

    private int applyRules(PlayerCharacter target, PlayerCharacter source, UUID sourceFeatureId, UUID sourceItemInstanceId,
                           List<FeatureRule> rules) {
        if (rules.isEmpty()) {
            return 0;
        }
        List<UUID> ruleIds = rules.stream().map(FeatureRule::getId).toList();
        List<FeatureEffectDefinition> defs = definitionRepository.findByFeatureRuleIdIn(ruleIds);
        if (defs.isEmpty()) {
            return 0;
        }
        FormulaContext ctx = contextFactory.build(source);
        int created = 0;
        for (FeatureEffectDefinition def : defs) {
            // same-feature reuse ends the previous instance
            List<FeatureEffectEndCondition> ends = endConditionRepository.findByEffectDefinitionId(def.getId());
            if (ends.stream().anyMatch(FeatureEffectEndCondition::isSameFeatureReuse)) {
                endActiveOfDefinition(target.getId(), def.getId());
            }
            applyStacking(target.getId(), def);

            Instant expiresAt = null;
            Integer remainingRounds = null;
            Integer durationValue = evaluateDuration(def, ctx);
            if (durationValue != null) {
                String unit = durationUnitCode(def);
                switch (unit) {
                    case "round" -> remainingRounds = durationValue;
                    case "minute" -> expiresAt = Instant.now().plus(Duration.ofMinutes(durationValue));
                    case "hour" -> expiresAt = Instant.now().plus(Duration.ofHours(durationValue));
                    case "day" -> expiresAt = Instant.now().plus(Duration.ofDays(durationValue));
                    default -> { /* until_rest/until_condition/permanent: ended by end conditions */ }
                }
            }

            // ABIL §3.1: если у эффекта есть состояние и цель в бою — вешаем его и запоминаем честную связь.
            UUID conditionInstanceId = conditionLinker.materialize(def, target.getId(), source.getId(), remainingRounds);

            activeRepository.save(FeatureActiveEffect.builder()
                    .characterId(target.getId())
                    .sourceCharacterId(source.getId())
                    .sourceFeatureId(sourceFeatureId)
                    .sourceItemInstanceId(sourceItemInstanceId)
                    .effectDefinitionId(def.getId())
                    .expiresAt(expiresAt)
                    .remainingRounds(remainingRounds)
                    .appliedConditionInstanceId(conditionInstanceId)
                    .status(ACTIVE)
                    .build());
            created++;
        }
        return created;
    }

    /**
     * Выполняет операции "end effect" в рамках бизнес-логики домена.
     * @param activeEffectId идентификатор active effect, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void endEffect(UUID activeEffectId) {
        activeRepository.findById(activeEffectId).ifPresent(effect -> {
            conditionLinker.clear(effect);
            effect.setStatus(ENDED);
            activeRepository.save(effect);
        });
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void applyStacking(UUID characterId, FeatureEffectDefinition def) {
        EffectStackingPolicy policy = EffectStackingPolicy.fromCode(def.getStackingPolicy())
                .orElse(EffectStackingPolicy.STACK);
        switch (policy) {
            case STACK -> { /* keep existing, add another */ }
            case REPLACE_SAME_FEATURE, HIGHEST_ONLY -> endActiveOfDefinition(characterId, def.getId());
            case REPLACE_SAME_GROUP -> endActiveOfGroup(characterId, def.getActiveEffectGroup());
        }
    }

    private void endActiveOfDefinition(UUID characterId, UUID definitionId) {
        activeRepository.findByCharacterIdAndEffectDefinitionIdAndStatus(characterId, definitionId, ACTIVE)
                .forEach(effect -> {
                    conditionLinker.clear(effect);
                    effect.setStatus(ENDED);
                    activeRepository.save(effect);
                });
    }

    private void endActiveOfGroup(UUID characterId, String group) {
        if (group == null || group.isBlank()) {
            return;
        }
        List<UUID> defIdsInGroup = definitionRepository.findAll().stream()
                .filter(d -> group.equals(d.getActiveEffectGroup()))
                .map(FeatureEffectDefinition::getId)
                .toList();
        activeRepository.findByCharacterIdAndStatus(characterId, ACTIVE).stream()
                .filter(e -> defIdsInGroup.contains(e.getEffectDefinitionId()))
                .forEach(e -> {
                    conditionLinker.clear(e);
                    e.setStatus(ENDED);
                    activeRepository.save(e);
                });
    }

    private Integer evaluateDuration(FeatureEffectDefinition def, FormulaContext ctx) {
        if (def.getDurationFormulaId() == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(def.getDurationFormulaId()).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return Math.max(0, formulaService.evaluateInteger(formula, ctx));
        } catch (FormulaException e) {
            log.warn("Effect duration formula failed for definition {}: {}", def.getId(), e.getMessage());
            return null;
        }
    }

    private String durationUnitCode(FeatureEffectDefinition def) {
        if (def.getDurationUnitId() == null) {
            return "round";
        }
        return durationUnitRepository.findById(def.getDurationUnitId())
                .map(u -> u.getCode())
                .orElse("round");
    }
}
