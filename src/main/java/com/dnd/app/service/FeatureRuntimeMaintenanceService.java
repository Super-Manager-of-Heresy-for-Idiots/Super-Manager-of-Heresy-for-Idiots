package com.dnd.app.service;

import com.dnd.app.domain.featurerule.CharacterTransformation;
import com.dnd.app.dto.featurerule.FeatureMaintenanceResult;
import com.dnd.app.repository.CharacterTransformationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Класс FeatureRuntimeMaintenanceService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureRuntimeMaintenanceService {

    private final EffectExpirationService effectExpirationService;
    private final PendingGameplayPromptService pendingGameplayPromptService;
    private final CharacterTransformationRepository transformationRepository;

    /**
     * Выполняет операции "run cleanup" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureMaintenanceResult runCleanup() {
        int effects = effectExpirationService.expireDue();
        int prompts = pendingGameplayPromptService.expireDue();

        List<CharacterTransformation> stale = transformationRepository
                .findByStatusAndExpiresAtIsNotNullAndExpiresAtBefore("active", Instant.now());
        stale.forEach(t -> {
            t.setStatus("ended");
            transformationRepository.save(t);
        });

        return FeatureMaintenanceResult.builder()
                .expiredEffects(effects)
                .expiredPrompts(prompts)
                .endedStaleTransformations(stale.size())
                .build();
    }
}
