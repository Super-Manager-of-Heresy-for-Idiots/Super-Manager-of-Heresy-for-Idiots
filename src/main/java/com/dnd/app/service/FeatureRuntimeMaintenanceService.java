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
 * Feature-runtime cleanup (Stage 13): expire due active effects, expire pending prompts, and end stale
 * transformations. Idempotent and safe to run repeatedly; can be triggered from admin or wired to a
 * scheduler later. Does nothing meaningful until the runtime is used, so it is inert by default.
 */
@Service
@RequiredArgsConstructor
public class FeatureRuntimeMaintenanceService {

    private final EffectExpirationService effectExpirationService;
    private final PendingGameplayPromptService pendingGameplayPromptService;
    private final CharacterTransformationRepository transformationRepository;

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
