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

/** Ends feature effects by time, rest, combat rounds, or manual GM action. */
@Service
@RequiredArgsConstructor
public class EffectExpirationService {

    private static final String ACTIVE = ActiveEffectStatus.ACTIVE.getCode();
    private static final String EXPIRED = ActiveEffectStatus.EXPIRED.getCode();
    private static final String ENDED = ActiveEffectStatus.ENDED.getCode();

    private final FeatureActiveEffectRepository activeRepository;
    private final FeatureEffectEndConditionRepository endConditionRepository;
    private final RestTypeRepository restTypeRepository;

    /** Wall-clock sweep: mark active effects whose expiry has passed as expired. Returns the count. */
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

    /** End effects that expire on the given rest type (e.g. Rage/effects ending on a long rest). */
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

    /** Combat helper: decrement remaining rounds for a character's active effects; expire at zero. */
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

    @Transactional
    public void gmEnd(UUID effectId) {
        activeRepository.findById(effectId).ifPresent(e -> {
            e.setStatus(ENDED);
            activeRepository.save(e);
        });
    }

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
