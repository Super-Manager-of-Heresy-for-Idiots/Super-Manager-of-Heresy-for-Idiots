package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.dto.featurerule.CharacterFeatureResourceResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Feature resources: creation on feature grant, max recomputation, spending and manual adjustment.
 * Gated by {@code app.feature-rules.resources} (via the master switch). Shared pools resolve to one
 * character row per {@code shared_pool_key} so Channel-Divinity-like resources are not duplicated.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureResourceService {

    private final FeatureRulesProperties flags;
    private final FeatureResourceDefinitionRepository definitionRepository;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;

    /** Create resource state for the given (approved) rules' resource definitions, if missing. */
    @Transactional
    public void ensureResourcesForRules(PlayerCharacter character, Collection<UUID> ruleIds) {
        if (!flags.resourcesActive() || ruleIds.isEmpty()) {
            return;
        }
        List<FeatureResourceDefinition> defs = definitionRepository.findByFeatureRuleIdIn(ruleIds);
        if (defs.isEmpty()) {
            return;
        }
        FormulaContext ctx = contextFactory.build(character);
        for (FeatureResourceDefinition def : defs) {
            if (alreadyExists(character.getId(), def)) {
                continue; // shared pool already created, or this definition already has state
            }
            Integer max = computeMax(def, ctx);
            resourceRepository.save(CharacterFeatureResource.builder()
                    .characterId(character.getId())
                    .resourceDefinitionId(def.getId())
                    .sharedPoolKey(def.getSharedPoolKey())
                    .currentValue(max != null ? max : 0)
                    .maxValueSnapshot(max)
                    .lastResetAt(Instant.now())
                    .build());
        }
    }

    /** Recompute max for all of a character's resources (e.g. after level-up); clamp current to max. */
    @Transactional
    public void recalcMax(PlayerCharacter character) {
        if (!flags.resourcesActive()) {
            return;
        }
        List<CharacterFeatureResource> resources = resourceRepository.findByCharacterId(character.getId());
        if (resources.isEmpty()) {
            return;
        }
        FormulaContext ctx = contextFactory.build(character);
        for (CharacterFeatureResource res : resources) {
            FeatureResourceDefinition def = definitionRepository.findById(res.getResourceDefinitionId()).orElse(null);
            if (def == null) {
                continue;
            }
            Integer max = computeMax(def, ctx);
            res.setMaxValueSnapshot(max);
            if (max != null && res.getCurrentValue() != null && res.getCurrentValue() > max) {
                res.setCurrentValue(max);
            }
            resourceRepository.save(res);
        }
    }

    /** Spend {@code amount} from a resource; rejects going below 0 unless the definition allows it. */
    @Transactional
    public CharacterFeatureResource spend(UUID characterId, UUID resourceId, int amount) {
        CharacterFeatureResource res = require(characterId, resourceId);
        FeatureResourceDefinition def = definitionRepository.findById(res.getResourceDefinitionId()).orElse(null);
        boolean allowNegative = def != null && def.isAllowNegative();
        int next = (res.getCurrentValue() != null ? res.getCurrentValue() : 0) - amount;
        if (next < 0 && !allowNegative) {
            throw new BadRequestException("Недостаточно ресурса для использования");
        }
        res.setCurrentValue(next);
        return resourceRepository.save(res);
    }

    /** GM/manual set of a resource's current value (clamped to [0..max] when a max is known). */
    @Transactional
    public CharacterFeatureResource setValue(UUID characterId, UUID resourceId, int value) {
        CharacterFeatureResource res = require(characterId, resourceId);
        int v = Math.max(0, value);
        if (res.getMaxValueSnapshot() != null) {
            v = Math.min(v, res.getMaxValueSnapshot());
        }
        res.setCurrentValue(v);
        return resourceRepository.save(res);
    }

    @Transactional(readOnly = true)
    public List<CharacterFeatureResource> list(UUID characterId) {
        return resourceRepository.findByCharacterId(characterId);
    }

    @Transactional(readOnly = true)
    public List<CharacterFeatureResourceResponse> listResponses(UUID characterId) {
        List<CharacterFeatureResource> resources = resourceRepository.findByCharacterId(characterId);
        if (resources.isEmpty()) {
            return List.of();
        }
        List<UUID> defIds = resources.stream()
                .map(CharacterFeatureResource::getResourceDefinitionId).distinct().toList();
        Map<UUID, FeatureResourceDefinition> defs = definitionRepository.findAllById(defIds).stream()
                .collect(Collectors.toMap(FeatureResourceDefinition::getId, d -> d));
        return resources.stream().map(res -> {
            FeatureResourceDefinition def = defs.get(res.getResourceDefinitionId());
            return CharacterFeatureResourceResponse.builder()
                    .id(res.getId())
                    .resourceDefinitionId(res.getResourceDefinitionId())
                    .resourceKey(def != null ? def.getResourceKey() : null)
                    .displayName(def != null ? def.getDisplayName() : null)
                    .currentValue(res.getCurrentValue())
                    .maxValue(res.getMaxValueSnapshot())
                    .sharedPoolKey(res.getSharedPoolKey())
                    .allowNegative(def != null && def.isAllowNegative())
                    .lastResetAt(res.getLastResetAt())
                    .build();
        }).toList();
    }

    public CharacterFeatureResourceResponse toResponse(CharacterFeatureResource res) {
        FeatureResourceDefinition def = definitionRepository.findById(res.getResourceDefinitionId()).orElse(null);
        return CharacterFeatureResourceResponse.builder()
                .id(res.getId())
                .resourceDefinitionId(res.getResourceDefinitionId())
                .resourceKey(def != null ? def.getResourceKey() : null)
                .displayName(def != null ? def.getDisplayName() : null)
                .currentValue(res.getCurrentValue())
                .maxValue(res.getMaxValueSnapshot())
                .sharedPoolKey(res.getSharedPoolKey())
                .allowNegative(def != null && def.isAllowNegative())
                .lastResetAt(res.getLastResetAt())
                .build();
    }

    @Transactional(readOnly = true)
    public Optional<FeatureResourceDefinition> definition(UUID definitionId) {
        return definitionRepository.findById(definitionId);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private boolean alreadyExists(UUID characterId, FeatureResourceDefinition def) {
        if (def.getSharedPoolKey() != null && !def.getSharedPoolKey().isBlank()) {
            return resourceRepository
                    .findFirstByCharacterIdAndSharedPoolKey(characterId, def.getSharedPoolKey()).isPresent();
        }
        return resourceRepository
                .findByCharacterIdAndResourceDefinitionId(characterId, def.getId()).isPresent();
    }

    /** Evaluate the max formula; returns null (unbounded/manual) if there is none or it can't evaluate. */
    private Integer computeMax(FeatureResourceDefinition def, FormulaContext ctx) {
        if (def.getMaxFormulaId() == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(def.getMaxFormulaId()).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return Math.max(0, formulaService.evaluateInteger(formula, ctx));
        } catch (FormulaException e) {
            log.warn("Resource max formula failed for definition {}: {}", def.getId(), e.getMessage());
            return null;
        }
    }

    private CharacterFeatureResource require(UUID characterId, UUID resourceId) {
        CharacterFeatureResource res = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BadRequestException("Ресурс не найден: " + resourceId));
        if (!characterId.equals(res.getCharacterId())) {
            throw new BadRequestException("Ресурс не принадлежит этому персонажу");
        }
        return res;
    }
}
