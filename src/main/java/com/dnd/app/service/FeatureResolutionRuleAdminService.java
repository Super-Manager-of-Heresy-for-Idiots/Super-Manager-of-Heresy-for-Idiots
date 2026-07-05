package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResolutionRule;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.dto.featurerule.ResolutionMetadataResponse;
import com.dnd.app.dto.featurerule.ResolutionRuleAdminResponse;
import com.dnd.app.dto.featurerule.ResolutionRuleEditRequest;
import com.dnd.app.dto.featurerule.RuleRefOption;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BestiaryAbilityRepository;
import com.dnd.app.repository.FeatureResolutionRuleRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Admin CRUD for a SAVE_CHECK_ATTACK (resolution) rule in the Rule Workbench. */
@Service
@RequiredArgsConstructor
public class FeatureResolutionRuleAdminService {

    /** Fixed resolution kinds understood by the runtime (see {@code FeatureResolutionRule.resolutionType}). */
    private static final List<String> RESOLUTION_TYPES =
            List.of("saving_throw", "ability_check", "skill_check", "attack_roll", "contested_check");

    private final FeatureRuleRepository ruleRepository;
    private final FeatureResolutionRuleRepository resolutionRepository;
    private final BestiaryAbilityRepository abilityRepository;
    private final SkillRepository skillRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    @Transactional(readOnly = true)
    public ResolutionMetadataResponse metadata() {
        return ResolutionMetadataResponse.builder()
                .resolutionTypes(RESOLUTION_TYPES.stream()
                        .map(code -> RuleRefOption.builder().code(code).label(code).build())
                        .toList())
                .abilities(abilityRepository.findAllByHomebrewIsNull().stream()
                        .sorted(Comparator.comparing(a -> a.getNameRusloc() != null ? a.getNameRusloc() : a.getCode()))
                        .map(a -> RuleRefOption.builder()
                                .id(a.getId()).code(a.getCode())
                                .label(a.getNameRusloc() != null ? a.getNameRusloc() : a.getCode())
                                .build())
                        .toList())
                .skills(skillRepository.findAllByHomebrewIsNull().stream()
                        .sorted(Comparator.comparing(sk -> sk.getName() != null ? sk.getName() : ""))
                        .map(sk -> RuleRefOption.builder().id(sk.getId()).code(sk.getName()).label(sk.getName()).build())
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public ResolutionRuleAdminResponse get(UUID ruleId) {
        return resolutionRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toResponse).orElse(null);
    }

    @Transactional
    public ResolutionRuleAdminResponse upsert(UUID ruleId, ResolutionRuleEditRequest req) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        FeatureResolutionRule res = resolutionRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .orElseGet(() -> FeatureResolutionRule.builder().featureRuleId(rule.getId()).build());

        String type = RESOLUTION_TYPES.contains(req.getResolutionType()) ? req.getResolutionType() : "saving_throw";
        res.setResolutionType(type);
        res.setAbilityId(req.getAbilityId());
        res.setSkillId(req.getSkillId());
        res.setDcFormulaId(formulaHelper.upsert(res.getDcFormulaId(), req.getDcFormula(),
                FormulaResultType.INTEGER.getCode()));

        return toResponse(resolutionRepository.save(res));
    }

    private ResolutionRuleAdminResponse toResponse(FeatureResolutionRule res) {
        FeatureFormula dc = formulaHelper.find(res.getDcFormulaId());
        return ResolutionRuleAdminResponse.builder()
                .id(res.getId())
                .resolutionType(res.getResolutionType())
                .abilityId(res.getAbilityId())
                .skillId(res.getSkillId())
                .dcFormula(dc != null ? dc.getExpression() : null)
                .dcFormulaStatus(dc != null ? dc.getValidationStatus() : null)
                .dcFormulaMessage(dc != null ? dc.getValidationMessage() : null)
                .build();
    }
}
