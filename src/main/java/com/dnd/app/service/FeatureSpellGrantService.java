package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureSpellFilter;
import com.dnd.app.domain.featurerule.FeatureSpellGrant;
import com.dnd.app.dto.featurerule.FeatureSpellCastResult;
import com.dnd.app.dto.featurerule.FeatureSpellGrantResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureSpellFilterRepository;
import com.dnd.app.repository.FeatureSpellGrantRepository;
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
 * Feature spell integration (Stage 9): an additive overlay on the existing spell model. Exposes the spells a
 * character's features grant (with always-prepared / counts-against-known / free-cast / ability-override
 * flags) and handles a resource-backed free cast. It never duplicates the spellcasting model — the spellbook
 * merges this overlay on top. Gated by {@code app.feature-rules.spells}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureSpellGrantService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final FeatureSpellGrantRepository grantRepository;
    private final FeatureSpellFilterRepository filterRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final FeatureResourceService featureResourceService;

    @Transactional(readOnly = true)
    public List<FeatureSpellGrantResponse> listGrantedSpells(PlayerCharacter character) {
        if (!flags.spellsActive()) {
            return List.of();
        }
        List<ClassFeature> features = resolver.knownBaseClassFeatures(character.getId());
        if (features.isEmpty()) {
            return List.of();
        }
        Map<UUID, ClassFeature> featureById = features.stream()
                .collect(Collectors.toMap(ClassFeature::getId, Function.identity(), (a, b) -> a));
        List<FeatureRule> rules = resolver.approvedEnabledRules(featureById.keySet());
        if (rules.isEmpty()) {
            return List.of();
        }
        Map<UUID, FeatureRule> ruleById = rules.stream()
                .collect(Collectors.toMap(FeatureRule::getId, Function.identity(), (a, b) -> a));
        List<FeatureSpellGrant> grants = grantRepository.findByFeatureRuleIdIn(ruleById.keySet());
        if (grants.isEmpty()) {
            return List.of();
        }
        FormulaContext ctx = contextFactory.build(character);

        return grants.stream().map(g -> {
            FeatureRule rule = ruleById.get(g.getFeatureRuleId());
            if (rule == null) {
                return null;
            }
            ClassFeature feature = featureById.get(rule.getOwnerId());
            if (feature == null) {
                return null;
            }
            return FeatureSpellGrantResponse.builder()
                    .id(g.getId())
                    .featureId(feature.getId())
                    .featureName(feature.getTitle())
                    .spellId(g.getSpellId())
                    .countsAgainstKnown(g.isCountsAgainstKnown())
                    .alwaysPrepared(g.isAlwaysPrepared())
                    .castWithoutSlot(g.isCastWithoutSlot())
                    .usesResourceDefinitionId(g.getUsesResourceDefinitionId())
                    .spellcastingAbilityOverrideId(g.getSpellcastingAbilityOverrideId())
                    .filter(buildFilter(g.getSpellFilterId(), ctx))
                    .build();
        }).filter(r -> r != null).toList();
    }

    @Transactional
    public FeatureSpellCastResult castViaFeature(PlayerCharacter character, UUID grantId) {
        if (!flags.spellsActive()) {
            throw new BadRequestException("Runtime заклинаний умений отключён");
        }
        FeatureSpellGrant grant = grantRepository.findById(grantId)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание умения не найдено: " + grantId));

        // ensure the grant belongs to a feature the character actually has
        List<UUID> featureIds = resolver.knownBaseClassFeatures(character.getId()).stream()
                .map(ClassFeature::getId).toList();
        List<UUID> ruleIds = resolver.approvedEnabledRules(featureIds).stream()
                .map(FeatureRule::getId).toList();
        if (!ruleIds.contains(grant.getFeatureRuleId())) {
            throw new BadRequestException("Заклинание не принадлежит этому персонажу");
        }

        Integer spent = null;
        Integer remaining = null;
        String resourceKey = null;
        if (grant.getUsesResourceDefinitionId() != null) {
            FeatureResourceDefinition def =
                    resourceDefinitionRepository.findById(grant.getUsesResourceDefinitionId()).orElse(null);
            if (def != null) {
                CharacterFeatureResource res = findResource(character.getId(), def);
                if (res == null) {
                    throw new BadRequestException("Ресурс для каста не инициализирован");
                }
                CharacterFeatureResource updated = featureResourceService.spend(character.getId(), res.getId(), 1);
                spent = 1;
                remaining = updated.getCurrentValue();
                resourceKey = def.getResourceKey();
            }
        }

        return FeatureSpellCastResult.builder()
                .spellGrantId(grant.getId())
                .spellId(grant.getSpellId())
                .castWithoutSlot(grant.isCastWithoutSlot())
                .resourceKey(resourceKey)
                .resourceSpent(spent)
                .resourceRemaining(remaining)
                .message("Каст выполнен через умение")
                .build();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private FeatureSpellGrantResponse.Filter buildFilter(UUID filterId, FormulaContext ctx) {
        if (filterId == null) {
            return null;
        }
        FeatureSpellFilter f = filterRepository.findById(filterId).orElse(null);
        if (f == null) {
            return null;
        }
        return FeatureSpellGrantResponse.Filter.builder()
                .classId(f.getClassId())
                .schoolId(f.getSchoolId())
                .maxSpellLevel(evalInt(f.getMaxSpellLevelFormulaId(), ctx))
                .tag(f.getTag())
                .sourceFilter(f.getSourceFilter())
                .build();
    }

    private CharacterFeatureResource findResource(UUID characterId, FeatureResourceDefinition def) {
        if (def.getSharedPoolKey() != null && !def.getSharedPoolKey().isBlank()) {
            return resourceRepository.findFirstByCharacterIdAndSharedPoolKey(characterId, def.getSharedPoolKey())
                    .orElse(null);
        }
        return resourceRepository.findByCharacterIdAndResourceDefinitionId(characterId, def.getId()).orElse(null);
    }

    private Integer evalInt(UUID formulaId, FormulaContext ctx) {
        if (formulaId == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(formulaId).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return formulaService.evaluateInteger(formula, ctx);
        } catch (FormulaException e) {
            log.warn("Spell filter formula failed for {}: {}", formulaId, e.getMessage());
            return null;
        }
    }
}
