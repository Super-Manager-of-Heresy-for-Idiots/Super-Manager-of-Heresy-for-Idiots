package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.BattleCombatantCondition;
import com.dnd.app.domain.BestiaryCondition;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.response.CombatantConditionResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BattleCombatantConditionRepository;
import com.dnd.app.repository.BestiaryConditionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Single entry point for condition instances on battle combatants (apply / remove / round tick).
 * The catalogue lives in {@code bestiary_conditions}; instances live on {@code battle_combatant_condition}.
 * Kept as one service so later feature-rules effects can hang conditions off the same path (A8). Phase
 * 1.1 tracks the marker + duration only — advantage/disadvantage automation from conditions is Phase 2.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final BattleCombatantConditionRepository conditionRepository;
    private final BestiaryConditionRepository bestiaryConditionRepository;
    private final WebSocketEventService webSocketEventService;

    /**
     * Apply a condition to a combatant. Duplicate policy: a re-apply of the same condition refreshes
     * its source note and duration (upsert), rather than erroring — the GM re-marking is common.
     */
    @Transactional
    public List<CombatantConditionResponse> apply(UUID campaignId, BattleCombatant combatant, UUID conditionId,
                                                  String sourceText, Integer remainingRounds, UUID actorId, int currentRound) {
        BestiaryCondition condition = bestiaryConditionRepository.findById(conditionId)
                .orElseThrow(() -> new ResourceNotFoundException("Condition not found"));
        BattleCombatantCondition entity = conditionRepository
                .findByCombatantIdAndConditionId(combatant.getId(), conditionId)
                .orElseGet(() -> BattleCombatantCondition.builder().combatant(combatant).condition(condition).build());
        entity.setCondition(condition);
        entity.setSourceText(sourceText);
        entity.setRemainingRounds(remainingRounds);
        entity.setAppliedRound(currentRound);
        entity.setAppliedBy(actorId);
        conditionRepository.save(entity);
        return publish(campaignId, combatant.getId(), actorId);
    }

    /** Remove a condition from a combatant (no-op if it isn't present). */
    @Transactional
    public List<CombatantConditionResponse> remove(UUID campaignId, UUID combatantId, UUID conditionId, UUID actorId) {
        conditionRepository.findByCombatantIdAndConditionId(combatantId, conditionId)
                .ifPresent(conditionRepository::delete);
        return publish(campaignId, combatantId, actorId);
    }

    /**
     * Round-boundary tick: decrement finite durations and delete any that reach 0. Conditions with a
     * null duration (until removed) are untouched. Call from the one place a new round begins.
     */
    @Transactional
    public void tick(UUID battleId) {
        for (BattleCombatantCondition c : conditionRepository.findByCombatant_Battle_Id(battleId)) {
            Integer left = c.getRemainingRounds();
            if (left == null) {
                continue;
            }
            if (left <= 1) {
                conditionRepository.delete(c);
            } else {
                c.setRemainingRounds(left - 1);
                conditionRepository.save(c);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<CombatantConditionResponse> conditionsForCombatant(UUID combatantId) {
        return conditionRepository.findByCombatantId(combatantId).stream().map(this::toDto).toList();
    }

    private List<CombatantConditionResponse> publish(UUID campaignId, UUID combatantId, UUID actorId) {
        List<CombatantConditionResponse> list = conditionsForCombatant(combatantId);
        webSocketEventService.sendCampaignEvent(WebSocketEventType.COMBATANT_CONDITIONS_CHANGED, campaignId,
                Map.of("combatantId", combatantId, "conditions", list), actorId);
        return list;
    }

    private CombatantConditionResponse toDto(BattleCombatantCondition c) {
        return CombatantConditionResponse.builder()
                .conditionId(c.getCondition().getId())
                .code(c.getCondition().getCode())
                .name(c.getCondition().getNameRusloc())
                .sourceText(c.getSourceText())
                .remainingRounds(c.getRemainingRounds())
                .build();
    }
}
