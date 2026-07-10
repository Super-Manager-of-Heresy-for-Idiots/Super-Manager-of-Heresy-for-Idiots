package com.dnd.app.service;

import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectEndCondition;
import com.dnd.app.domain.featurerule.RestType;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectEndConditionRepository;
import com.dnd.app.repository.RestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс EffectExpirationService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class EffectExpirationService {

    private static final String ACTIVE = ActiveEffectStatus.ACTIVE.getCode();
    private static final String EXPIRED = ActiveEffectStatus.EXPIRED.getCode();
    private static final String ENDED = ActiveEffectStatus.ENDED.getCode();

    private final FeatureActiveEffectRepository activeRepository;
    private final FeatureEffectEndConditionRepository endConditionRepository;
    private final RestTypeRepository restTypeRepository;

    /**
     * Выполняет операции "expire due" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int expireDue() {
        List<FeatureActiveEffect> due =
                activeRepository.findByStatusAndExpiresAtIsNotNullAndExpiresAtBefore(ACTIVE, Instant.now());
        due.forEach(e -> {
            e.setStatus(EXPIRED);
            activeRepository.save(e);
        });
        return due.size();
    }

    /**
     * Выполняет операции "end on rest" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param restTypeCode входящее значение rest type code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int endOnRest(PlayerCharacter character, String restTypeCode) {
        RestType restType = restTypeRepository.findByCode(restTypeCode).orElse(null);
        if (restType == null) {
            return 0;
        }
        int ended = 0;
        for (FeatureActiveEffect effect : activeRepository.findByCharacterIdAndStatus(character.getId(), ACTIVE)) {
            List<FeatureEffectEndCondition> ends =
                    endConditionRepository.findByEffectDefinitionId(effect.getEffectDefinitionId());
            boolean endsOnRest = ends.stream().anyMatch(c -> restType.getId().equals(c.getRestTypeId()));
            if (endsOnRest) {
                effect.setStatus(ENDED);
                activeRepository.save(effect);
                ended++;
            }
        }
        return ended;
    }

    /**
     * Выполняет операции "tick rounds" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void tickRounds(UUID characterId) {
        for (FeatureActiveEffect effect : activeRepository.findByCharacterIdAndStatus(characterId, ACTIVE)) {
            if (effect.getRemainingRounds() == null) {
                continue;
            }
            int left = effect.getRemainingRounds() - 1;
            if (left <= 0) {
                effect.setRemainingRounds(0);
                effect.setStatus(EXPIRED);
            } else {
                effect.setRemainingRounds(left);
            }
            activeRepository.save(effect);
        }
    }

    /**
     * Выполняет операции "gm end" в рамках бизнес-логики домена.
     * @param effectId идентификатор effect, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void gmEnd(UUID effectId) {
        activeRepository.findById(effectId).ifPresent(e -> {
            e.setStatus(ENDED);
            activeRepository.save(e);
        });
    }

    /**
     * Выполняет операции "gm set rounds" в рамках бизнес-логики домена.
     * @param effectId идентификатор effect, используемый для выбора нужного бизнес-объекта
     * @param rounds входящее значение rounds, используемое бизнес-сценарием
     */
    @Transactional
    public void gmSetRounds(UUID effectId, int rounds) {
        activeRepository.findById(effectId).ifPresent(e -> {
            if (rounds <= 0) {
                e.setRemainingRounds(0);
                e.setStatus(EXPIRED);
            } else {
                e.setRemainingRounds(rounds);
            }
            activeRepository.save(e);
        });
    }
}
