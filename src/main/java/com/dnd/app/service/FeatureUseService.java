package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureUseLog;
import com.dnd.app.dto.featurerule.FeatureUseRequest;
import com.dnd.app.dto.featurerule.FeatureUseResult;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureUseLogRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Executes a feature use: validates the character has the feature and an approved action cost, spends the
 * feature's resource (if any), and records an audit log entry. Gated by {@code app.feature-rules.actions}.
 *
 * <p>Stage 6 scope: resource + action-cost handling + audit. In-combat action-economy enforcement and the
 * typed FeatureUsed event/reaction bus arrive in later stages (8 and 11).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureUseService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final FeatureActionCostRepository actionCostRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final FeatureResourceDefinitionRepository resourceDefinitionRepository;
    private final CharacterFeatureResourceRepository resourceRepository;
    private final FeatureResourceService featureResourceService;
    private final FeatureActionService featureActionService;
    private final CharacterFormulaContextFactory contextFactory;
    private final FeatureUseLogRepository useLogRepository;

    @Transactional
    public FeatureUseResult use(PlayerCharacter character, UUID featureId, FeatureUseRequest request) {
        if (!flags.actionsActive()) {
            throw new BadRequestException("Runtime использования умений отключён");
        }
        ClassFeature feature = resolver.knownBaseClassFeatures(character.getId()).stream()
                .filter(f -> f.getId().equals(featureId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Персонаж не обладает этим умением"));

        List<FeatureRule> rules = resolver.approvedEnabledRules(List.of(featureId));
        if (rules.isEmpty()) {
            throw new BadRequestException("У умения нет утверждённых правил");
        }
        List<UUID> ruleIds = rules.stream().map(FeatureRule::getId).toList();

        List<FeatureActionCost> costs = actionCostRepository.findByFeatureRuleIdIn(ruleIds);
        if (costs.isEmpty()) {
            throw new BadRequestException("У умения нет стоимости действия (action cost)");
        }
        FeatureActionCost cost = costs.get(0);
        ActionType actionType = actionTypeRepository.findById(cost.getActionTypeId()).orElse(null);

        Integer spent = null;
        Integer remaining = null;
        String resourceKey = null;

        FeatureResourceDefinition def = resourceDefinitionRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .findFirst().orElse(null);
        if (def != null) {
            FormulaContext ctx = contextFactory.build(character);
            int amount = featureActionService.spendPerUse(def, ctx);
            CharacterFeatureResource res = findResource(character.getId(), def);
            if (res == null) {
                throw new BadRequestException("Ресурс умения не инициализирован");
            }
            CharacterFeatureResource updated = featureResourceService.spend(character.getId(), res.getId(), amount);
            spent = amount;
            remaining = updated.getCurrentValue();
            resourceKey = def.getResourceKey();
        }

        UUID combatId = request != null ? request.getCombatId() : null;
        FeatureUseLog logEntry = useLogRepository.save(FeatureUseLog.builder()
                .characterId(character.getId())
                .featureId(feature.getId())
                .featureRuleId(cost.getFeatureRuleId())
                .combatId(combatId)
                .actionType(actionType != null ? actionType.getCode() : null)
                .resourceSpent(spent)
                .detail(feature.getTitle())
                .build());

        return FeatureUseResult.builder()
                .featureId(feature.getId())
                .featureName(feature.getTitle())
                .actionType(actionType != null ? actionType.getCode() : null)
                .resourceKey(resourceKey)
                .resourceSpent(spent)
                .resourceRemaining(remaining)
                .logId(logEntry.getId())
                .message("Умение использовано")
                .build();
    }

    private CharacterFeatureResource findResource(UUID characterId, FeatureResourceDefinition def) {
        if (def.getSharedPoolKey() != null && !def.getSharedPoolKey().isBlank()) {
            return resourceRepository.findFirstByCharacterIdAndSharedPoolKey(characterId, def.getSharedPoolKey())
                    .orElse(null);
        }
        return resourceRepository.findByCharacterIdAndResourceDefinitionId(characterId, def.getId()).orElse(null);
    }
}
