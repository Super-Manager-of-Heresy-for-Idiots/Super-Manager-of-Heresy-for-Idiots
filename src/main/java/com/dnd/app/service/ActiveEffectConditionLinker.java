package com.dnd.app.service;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * ABIL §3.1: связывает feature-rules эффект с состоянием боя (battle_combatant_condition). Общий сотрудник
 * для {@link FeatureEffectService} (наложение/снятие эффекта) и {@link EffectExpirationService} (истечение/отдых/GM),
 * чтобы логика материализации и снятия состояния жила в одном месте и не дублировалась.
 *
 * Материализация: если у определения эффекта есть condition-модификатор и цель — комбатант АКТИВНОГО боя,
 * состояние вешается и его id инстанса возвращается для честной связи. Снятие: снимает связанное состояние,
 * но только если его не держит ещё один активный эффект (общая строка condition идемпотентна по паре
 * комбатант+состояние). Цель не в бою → эффект живёт как обычный баф/дебаф без боевого состояния.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveEffectConditionLinker {

    private static final String ACTIVE_EFFECT = ActiveEffectStatus.ACTIVE.getCode();

    private final ConditionService conditionService;
    private final FeatureActiveEffectRepository activeRepository;
    private final FeatureEffectModifierRepository modifierRepository;
    private final BattleCombatantRepository battleCombatantRepository;

    /**
     * Вешает состояние от эффекта, если оно предусмотрено и цель в активном бою.
     * @param def определение эффекта (для поиска condition-модификатора)
     * @param targetCharacterId держатель эффекта (цель)
     * @param sourceCharacterId источник (кастующий) — для WS-события
     * @param remainingRounds длительность эффекта в раундах (переносится на состояние)
     * @return id инстанса наложенного состояния для честной связи, либо {@code null}
     */
    public UUID materialize(FeatureEffectDefinition def, UUID targetCharacterId, UUID sourceCharacterId,
                            Integer remainingRounds) {
        UUID conditionId = modifierRepository.findByEffectDefinitionId(def.getId()).stream()
                .map(FeatureEffectModifier::getConditionId)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (conditionId == null) {
            return null; // у эффекта нет состояния — обычный числовой баф/дебаф
        }
        List<BattleCombatant> combatants = battleCombatantRepository
                .findByCharacter_IdAndBattle_Status(targetCharacterId, BattleStatus.ACTIVE);
        if (combatants.isEmpty()) {
            return null; // цель не в активном бою — состояние навесить некуда
        }
        BattleCombatant combatant = combatants.get(0);
        Battle battle = combatant.getBattle();
        UUID instanceId = conditionService.applyForEffect(battle.getCampaign().getId(), combatant, conditionId,
                remainingRounds, sourceCharacterId, battle.getRoundNumber());
        log.info("Effect materialized condition: defId={}, targetId={}, conditionId={}, instanceId={}",
                def.getId(), targetCharacterId, conditionId, instanceId);
        return instanceId;
    }

    /**
     * Снимает состояние, наложенное завершающимся эффектом — но только если его не держит ещё один активный эффект.
     * Идемпотентно: если инстанс уже удалён (GM снял руками), это no-op. Обнуляет ссылку у переданного эффекта.
     * @param effect завершающийся эффект (мутируется: {@code appliedConditionInstanceId} → null)
     */
    public void clear(FeatureActiveEffect effect) {
        UUID instanceId = effect.getAppliedConditionInstanceId();
        if (instanceId == null) {
            return;
        }
        effect.setAppliedConditionInstanceId(null);
        long othersHolding = activeRepository
                .countByAppliedConditionInstanceIdAndStatusAndIdNot(instanceId, ACTIVE_EFFECT, effect.getId());
        if (othersHolding == 0) {
            conditionService.removeByInstanceId(instanceId, effect.getSourceCharacterId());
        }
    }
}
