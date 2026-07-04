package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.CharacterFeatureCompanion;
import com.dnd.app.domain.featurerule.CharacterTransformation;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.dto.featurerule.CompanionResponse;
import com.dnd.app.dto.featurerule.TacticalSnapshot;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterFeatureCompanionRepository;
import com.dnd.app.repository.CharacterTransformationRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.dnd.app.service.formula.FormulaException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Feature companions + the tactical snapshot the frontend/map-service consumes. Companion HP/AC/attack are
 * computed from formulas against the owner's context. Gated by {@code app.feature-rules.forms}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureCompanionService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureCompanionRepository companionRepository;
    private final CharacterTransformationRepository transformationRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;
    private final CharacterFormulaContextFactory contextFactory;

    @Transactional
    public CompanionResponse createCompanion(PlayerCharacter character, UUID monsterId,
                                             UUID sourceFeatureId, String customName) {
        if (!flags.formsActive()) {
            throw new BadRequestException("Runtime спутников отключён");
        }
        CharacterFeatureCompanion companion = companionRepository.save(CharacterFeatureCompanion.builder()
                .characterId(character.getId())
                .monsterId(monsterId)
                .sourceFeatureId(sourceFeatureId)
                .customName(customName)
                .state("active")
                .build());
        return toResponse(companion, contextFactory.build(character));
    }

    @Transactional(readOnly = true)
    public List<CompanionResponse> listCompanions(PlayerCharacter character) {
        FormulaContext ctx = contextFactory.build(character);
        return companionRepository.findByCharacterId(character.getId()).stream()
                .map(c -> toResponse(c, ctx)).toList();
    }

    @Transactional
    public void dismissCompanion(UUID companionId) {
        companionRepository.findById(companionId).ifPresent(c -> {
            c.setState("dismissed");
            companionRepository.save(c);
        });
    }

    @Transactional(readOnly = true)
    public TacticalSnapshot tacticalSnapshot(PlayerCharacter character) {
        FormulaContext ctx = contextFactory.build(character);
        CharacterTransformation active = transformationRepository
                .findByCharacterIdAndStatus(character.getId(), "active").stream().findFirst().orElse(null);
        List<CompanionResponse> companions = companionRepository.findByCharacterId(character.getId()).stream()
                .filter(c -> "active".equals(c.getState()))
                .map(c -> toResponse(c, ctx)).toList();
        return TacticalSnapshot.builder()
                .characterId(character.getId())
                .activeTransformationMonsterId(active != null ? active.getMonsterId() : null)
                .activeTransformationId(active != null ? active.getId() : null)
                .companions(companions)
                .build();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private CompanionResponse toResponse(CharacterFeatureCompanion c, FormulaContext ctx) {
        return CompanionResponse.builder()
                .id(c.getId()).monsterId(c.getMonsterId()).customName(c.getCustomName()).state(c.getState())
                .hp(evalInt(c.getHpFormulaId(), ctx))
                .ac(evalInt(c.getAcFormulaId(), ctx))
                .attackBonus(evalInt(c.getAttackBonusFormulaId(), ctx))
                .build();
    }

    private Integer evalInt(UUID formulaId, FormulaContext ctx) {
        if (formulaId == null) {
            return null;
        }
        FeatureFormula formula = formulaRepository.findById(formulaId).orElse(null);
        if (formula == null) {
            return null;
        }
        try {
            return formulaService.evaluateInteger(formula, ctx);
        } catch (FormulaException e) {
            log.warn("Companion formula failed for {}: {}", formulaId, e.getMessage());
            return null;
        }
    }
}
