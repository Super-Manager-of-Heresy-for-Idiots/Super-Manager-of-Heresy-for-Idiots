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
 * Класс ConditionService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConditionService {

    private final BattleCombatantConditionRepository conditionRepository;
    private final BestiaryConditionRepository bestiaryConditionRepository;
    private final WebSocketEventService webSocketEventService;

    /**
     * Выполняет операции "apply" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param combatant входящее значение combatant, используемое бизнес-сценарием
     * @param conditionId идентификатор condition, используемый для выбора нужного бизнес-объекта
     * @param sourceText входящее значение source text, используемое бизнес-сценарием
     * @param remainingRounds входящее значение remaining rounds, используемое бизнес-сценарием
     * @param actorId идентификатор actor, используемый для выбора нужного бизнес-объекта
     * @param currentRound входящее значение current round, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
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

    /**
     * Выполняет операции "apply by code" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param combatant входящее значение combatant, используемое бизнес-сценарием
     * @param code входящее значение code, используемое бизнес-сценарием
     * @param actorId идентификатор actor, используемый для выбора нужного бизнес-объекта
     * @param currentRound входящее значение current round, используемое бизнес-сценарием
     */
    @Transactional
    public void applyByCode(UUID campaignId, BattleCombatant combatant, String code, UUID actorId, int currentRound) {
        bestiaryConditionRepository.findByCodeAndHomebrewIsNull(code)
                .ifPresent(cond -> apply(campaignId, combatant, cond.getId(), null, null, actorId, currentRound));
    }

    /**
     * Удаляет результат операции "remove by code" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param code входящее значение code, используемое бизнес-сценарием
     * @param actorId идентификатор actor, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void removeByCode(UUID campaignId, UUID combatantId, String code, UUID actorId) {
        bestiaryConditionRepository.findByCodeAndHomebrewIsNull(code)
                .ifPresent(cond -> remove(campaignId, combatantId, cond.getId(), actorId));
    }

    /**
     * Удаляет результат операции "remove" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param conditionId идентификатор condition, используемый для выбора нужного бизнес-объекта
     * @param actorId идентификатор actor, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public List<CombatantConditionResponse> remove(UUID campaignId, UUID combatantId, UUID conditionId, UUID actorId) {
        conditionRepository.findByCombatantIdAndConditionId(combatantId, conditionId)
                .ifPresent(conditionRepository::delete);
        return publish(campaignId, combatantId, actorId);
    }

    /**
     * Выполняет операции "tick" в рамках бизнес-логики домена.
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
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

    /**
     * Выполняет операции "conditions for combatant" в рамках бизнес-логики домена.
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
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
