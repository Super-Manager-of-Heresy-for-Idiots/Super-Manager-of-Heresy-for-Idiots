package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Класс CharacterHpService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterHpService {

    private final PlayerCharacterRepository characterRepository;
    private final BattleCombatantRepository combatantRepository;
    private final WebSocketEventService webSocketEventService;
    private final GameplayEventService gameplayEventService;

    /**
     * Выполняет операции "apply delta" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param delta входящее значение delta, используемое бизнес-сценарием
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HpChangeResult applyDelta(UUID characterId, int delta, UUID campaignId, UUID actorUserId) {
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        Integer maxHpObj = character.getMaxHp();
        int cap = maxHpObj != null ? maxHpObj : 0;
        int before = character.getCurrentHp() != null ? character.getCurrentHp() : 0;

        character.applyHpDelta(delta, cap);
        characterRepository.save(character);

        int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
        int tempHp = character.getTempHp() != null ? character.getTempHp() : 0;

        syncCombatants(characterId, currentHp, maxHpObj);
        broadcast(character.getId(), campaignId, actorUserId, currentHp, tempHp, cap);

        boolean reachedZero = before > 0 && currentHp <= 0;
        if (reachedZero) {
            // Durable hook so death saves / concentration can attach later; no-op unless triggers on.
            gameplayEventService.publish(character, "hp_reached_zero", null,
                    "{\"characterId\":\"" + characterId + "\"}");
        }
        return new HpChangeResult(characterId, currentHp, tempHp, cap, reachedZero);
    }

    /**
     * Выполняет операции "apply damage" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param amount входящее значение amount, используемое бизнес-сценарием
     * @param damageTypeId идентификатор damage type, используемый для выбора нужного бизнес-объекта
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HpChangeResult applyDamage(UUID characterId, int amount, UUID damageTypeId,
                                      UUID campaignId, UUID actorUserId) {
        return applyDelta(characterId, -Math.max(0, amount), campaignId, actorUserId);
    }

    /**
     * Выполняет операции "apply temp hp" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param amount входящее значение amount, используемое бизнес-сценарием
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HpChangeResult applyTempHp(UUID characterId, int amount, UUID campaignId, UUID actorUserId) {
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        character.grantTempHp(Math.max(0, amount));
        characterRepository.save(character);

        int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
        int tempHp = character.getTempHp() != null ? character.getTempHp() : 0;
        int cap = character.getMaxHp() != null ? character.getMaxHp() : 0;

        broadcast(character.getId(), campaignId, actorUserId, currentHp, tempHp, cap);
        return new HpChangeResult(characterId, currentHp, tempHp, cap, false);
    }

    /**
     * Выполняет операции "restore to full" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param actorUserId идентификатор actor user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HpChangeResult restoreToFull(UUID characterId, UUID campaignId, UUID actorUserId) {
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        Integer maxObj = character.getMaxHp();
        int full = maxObj != null ? maxObj : (character.getCurrentHp() != null ? character.getCurrentHp() : 0);
        character.setCurrentHp(full);
        character.setTempHp(0);
        characterRepository.save(character);

        syncCombatants(characterId, full, maxObj);
        broadcast(character.getId(), campaignId, actorUserId, full, 0, full);
        return new HpChangeResult(characterId, full, 0, maxObj != null ? maxObj : full, false);
    }

    private void syncCombatants(UUID characterId, int currentHp, Integer maxHp) {
        for (BattleCombatant combatant :
                combatantRepository.findByCharacter_IdAndBattle_Status(characterId, BattleStatus.ACTIVE)) {
            combatant.setCurrentHp(currentHp);
            if (maxHp != null) {
                combatant.setMaxHp(maxHp);
            }
            combatantRepository.save(combatant);
        }
    }

    private void broadcast(UUID characterId, UUID campaignId, UUID actorUserId,
                           int currentHp, int tempHp, int maxHp) {
        webSocketEventService.sendCampaignEvent(WebSocketEventType.HP_CHANGED, campaignId, characterId,
                Map.of("currentHp", currentHp, "tempHp", tempHp, "maxHp", maxHp), actorUserId);
    }
}
