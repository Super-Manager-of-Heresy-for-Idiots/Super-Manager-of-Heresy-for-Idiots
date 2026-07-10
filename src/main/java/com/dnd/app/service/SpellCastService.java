package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureUseLog;
import com.dnd.app.dto.featurerule.FeatureApplyResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.SpellCastRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureUseLogRepository;
import com.dnd.app.repository.SpellRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс SpellCastService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellCastService {

    private final FeatureRulesProperties flags;
    private final SpellRepository spellRepository;
    private final CharacterKnownSpellRepository knownSpellRepository;
    private final CharacterFeatureResolver resolver;
    private final FeatureActionCostRepository actionCostRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final CombatActionEconomyService economyService;
    private final SpellSlotService slotService;
    private final CombatFeatureExecutionService executionService;
    private final FeatureEffectService effectService;
    private final GameplayEventService gameplayEventService;
    private final FeatureUseLogRepository useLogRepository;

    /**
     * Применяет заклинание операции "cast" в рамках бизнес-логики домена.
     * @param caster входящее значение caster, используемое бизнес-сценарием
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param effectTarget входящее значение effect target, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public SpellCastResult cast(PlayerCharacter caster, UUID spellId, SpellCastRequest request,
                                PlayerCharacter effectTarget) {
        if (!flags.spellsActive()) {
            throw new BadRequestException("Runtime заклинаний отключён");
        }
        Spell spell = requireSpell(spellId);
        if (!knownSpellRepository.existsByCharacterIdAndSpellId(caster.getId(), spellId)) {
            throw new BadRequestException("Персонаж не знает это заклинание");
        }
        List<FeatureRule> rules =
                resolver.approvedEnabledRules(FeatureRuleOwnerType.SPELL, List.of(spellId));

        // 1. Combat action economy FIRST: if the declared slot is already spent this turn, the whole
        //    cast (including the spell slot below) rolls back untouched.
        UUID combatId = request != null ? request.getCombatId() : null;
        String actionCode = actionCode(rules);
        if (combatId != null && actionCode != null) {
            economyService.spend(combatId, caster.getId(), actionCode);
        }

        // 2. Spell slot (cantrips are free). Upcasting: any slot of the spell's level or higher.
        Integer slotLevel = null;
        SpellSlotsResponse slots = null;
        int spellLevel = spell.getLevel() != null ? spell.getLevel() : 0;
        if (spellLevel > 0) {
            slotLevel = request != null && request.getSlotLevel() != null
                    ? request.getSlotLevel() : spellLevel;
            if (slotLevel < spellLevel) {
                throw new BadRequestException(
                        "Ячейка " + slotLevel + "-го уровня ниже уровня заклинания (" + spellLevel + ")");
            }
            slots = slotService.expendInternal(caster.getId(), slotLevel);
        }

        // 3. The shared engine computes what to roll; the slot level feeds spell_slot_level formulas.
        FeatureExecutionPlan plan = executionService.planForSpell(caster, spell, slotLevel);

        // 4. Active effects (backfilled from spell_buffs) land on the chosen target, caster by default.
        PlayerCharacter target = effectTarget != null ? effectTarget : caster;
        int effectsApplied = effectService.applyForSpellCast(caster, target, spellId);

        gameplayEventService.publish(caster, "spell_cast", combatId,
                "{\"spellId\":\"" + spellId + "\",\"slotLevel\":" + slotLevel + "}");
        useLogRepository.save(FeatureUseLog.builder()
                .characterId(caster.getId())
                .featureRuleId(rules.stream().map(FeatureRule::getId).findFirst().orElse(null))
                .combatId(combatId)
                .actionType(actionCode != null ? actionCode : "spell_cast")
                .detail("spell=" + spell.getSlug() + (slotLevel != null ? ", slot=L" + slotLevel : ""))
                .build());

        return SpellCastResult.builder()
                .spellId(spellId)
                .spellName(spell.getNameRu())
                .slotLevelUsed(slotLevel)
                .actionType(combatId != null ? actionCode : null)
                .effectsApplied(effectsApplied)
                .plan(plan)
                .slots(slots)
                .message("Заклинание использовано")
                .build();
    }

    /**
     * Выполняет операции "apply to target" в рамках бизнес-логики домена.
     * @param caster входящее значение caster, используемое бизнес-сценарием
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param target входящее значение target, используемое бизнес-сценарием
     * @param damage входящее значение damage, используемое бизнес-сценарием
     * @param healing входящее значение healing, используемое бизнес-сценарием
     * @param damageTypeId идентификатор damage type, используемый для выбора нужного бизнес-объекта
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureApplyResult applyToTarget(
            PlayerCharacter caster, UUID spellId, PlayerCharacter target,
            Integer damage, Integer healing, UUID damageTypeId, UUID campaignId, UUID actorUserId) {
        Spell spell = requireSpell(spellId);
        return executionService.applySpellToTarget(
                caster, spell, target, damage, healing, damageTypeId, campaignId, actorUserId);
    }

    /**
     * Выполняет операции "plan" в рамках бизнес-логики домена.
     * @param caster входящее значение caster, используемое бизнес-сценарием
     * @param spellId идентификатор spell, используемый для выбора нужного бизнес-объекта
     * @param slotLevel входящее значение slot level, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatureExecutionPlan plan(PlayerCharacter caster, UUID spellId, Integer slotLevel) {
        Spell spell = requireSpell(spellId);
        int spellLevel = spell.getLevel() != null ? spell.getLevel() : 0;
        Integer effective = spellLevel > 0
                ? (slotLevel != null ? slotLevel : spellLevel)
                : null;
        return executionService.planForSpell(caster, spell, effective);
    }

    /**
     * The action-economy code of the spell's approved {@code action_cost} rule. Null when the spell has
     * no approved cost data — the runtime then follows the FR philosophy that unapproved rules have no
     * gameplay effect (no cost is guessed).
     */
    private String actionCode(List<FeatureRule> rules) {
        if (rules.isEmpty()) {
            return null;
        }
        List<UUID> ruleIds = rules.stream().map(FeatureRule::getId).toList();
        return actionCostRepository.findByFeatureRuleIdIn(ruleIds).stream()
                .findFirst()
                .map(FeatureActionCost::getActionTypeId)
                .flatMap(actionTypeRepository::findById)
                .map(ActionType::getCode)
                .orElse(null);
    }

    private Spell requireSpell(UUID spellId) {
        return spellRepository.findById(spellId)
                .orElseThrow(() -> new ResourceNotFoundException("Заклинание не найдено: " + spellId));
    }
}
