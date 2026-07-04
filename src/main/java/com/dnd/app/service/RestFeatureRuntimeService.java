package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.RestType;
import com.dnd.app.dto.featurerule.RestResourcePreview;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
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
 * Rest recovery for feature resources: preview what a short/long rest (or other reset window) would
 * restore, and apply it. Resources with a {@code reset_amount_formula} regain that many (capped at max);
 * otherwise a matching rest fully restores to max. Gated by {@code app.feature-rules.resources}.
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

    @Transactional(readOnly = true)
    public List<RestResourcePreview> preview(PlayerCharacter character, String restTypeCode) {
        return compute(character, restTypeCode, false);
    }

    @Transactional
    public List<RestResourcePreview> complete(PlayerCharacter character, String restTypeCode) {
        List<RestResourcePreview> restored = compute(character, restTypeCode, true);
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

    /** Target value after reset: current + reset_amount (capped at max), or full restore to max. */
    private Integer resetTarget(CharacterFeatureResource res, FeatureResourceDefinition def, FormulaContext ctx) {
        Integer max = res.getMaxValueSnapshot();
        if (def.getResetAmountFormulaId() != null) {
            FeatureFormula formula = formulaRepository.findById(def.getResetAmountFormulaId()).orElse(null);
            if (formula != null) {
                try {
                    int amount = formulaService.evaluateInteger(formula, ctx);
                    int current = res.getCurrentValue() != null ? res.getCurrentValue() : 0;
                    int target = current + amount;
                    return max != null ? Math.min(target, max) : target;
                } catch (FormulaException e) {
                    log.warn("Reset amount formula failed for resource {}: {}", res.getId(), e.getMessage());
                }
            }
        }
        return max; // full restore (null if max unknown → caller skips)
    }
}
