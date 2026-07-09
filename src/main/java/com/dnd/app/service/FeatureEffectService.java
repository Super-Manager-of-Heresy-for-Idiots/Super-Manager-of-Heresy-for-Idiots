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
 * Applies and replaces active feature effects when a feature is used (stacking policy, duration from a
 * formula/unit, same-feature-reuse end). Gated by {@code app.feature-rules.effects}. This is a separate
 * lifecycle layer from the existing item/spell buff system.
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

    /** Create active effects for the effect definitions of a used feature. Returns the number created. */
    @Transactional
    public int applyForFeatureUse(PlayerCharacter character, UUID featureId) {
        if (!flags.effectsActive()) {
            return 0; // gate before any resolution work: disabled must stay a strict no-op
        }
        List<FeatureRule> rules = resolver.approvedEnabledRules(List.of(featureId));
        return applyRules(character, character, featureId, rules);
    }

    /**
     * Create active effects of a cast spell (rules with {@code owner_type = SPELL}) on {@code target}.
     * The duration formulas run in the CASTER's context (the caster's power drives the effect), while the
     * effect itself lands on the target. {@code source_feature_id} stays null — its FK points at
     * {@code class_feature}; the spell is traceable through the definition's rule (owner SPELL).
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
        return applyRules(target, caster, null, rules);
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

    /** True if the caster has any ACTIVE concentration-required effect (a spell they're concentrating on). */
    @Transactional(readOnly = true)
    public boolean isConcentrating(UUID casterId) {
        return !concentrationEffects(casterId).isEmpty();
    }

    /**
     * End all of the caster's ACTIVE concentration-required effects — concentration broken (damage/save
     * fail, 0 HP, or a new concentration spell) or dropped. Returns how many effects ended. Uses the same
     * {@code status = ENDED} termination as every other effect end; no parallel mechanism.
     */
    @Transactional
    public int endConcentration(UUID casterId) {
        List<FeatureActiveEffect> concentration = concentrationEffects(casterId);
        for (FeatureActiveEffect effect : concentration) {
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

    private int applyRules(PlayerCharacter target, PlayerCharacter source, UUID sourceFeatureId,
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

            activeRepository.save(FeatureActiveEffect.builder()
                    .characterId(target.getId())
                    .sourceCharacterId(source.getId())
                    .sourceFeatureId(sourceFeatureId)
                    .effectDefinitionId(def.getId())
                    .expiresAt(expiresAt)
                    .remainingRounds(remainingRounds)
                    .status(ACTIVE)
                    .build());
            created++;
        }
        return created;
    }

    @Transactional
    public void endEffect(UUID activeEffectId) {
        activeRepository.findById(activeEffectId).ifPresent(effect -> {
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
