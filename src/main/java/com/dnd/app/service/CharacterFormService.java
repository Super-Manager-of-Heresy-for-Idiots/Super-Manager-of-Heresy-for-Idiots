package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.CharacterKnownForm;
import com.dnd.app.domain.featurerule.CharacterTransformation;
import com.dnd.app.domain.featurerule.FeatureAllowedMonsterFilter;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.featurerule.KnownFormResponse;
import com.dnd.app.dto.featurerule.MonsterEligibilityResult;
import com.dnd.app.dto.featurerule.TransformationResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterKnownFormRepository;
import com.dnd.app.repository.CharacterTransformationRepository;
import com.dnd.app.repository.FeatureAllowedMonsterFilterRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс CharacterFormService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class CharacterFormService {

    private final FeatureRulesProperties flags;
    private final CharacterFeatureResolver resolver;
    private final FeatureAllowedMonsterFilterRepository filterRepository;
    private final FeatureMonsterEligibilityService eligibilityService;
    private final CharacterKnownFormRepository knownFormRepository;
    private final CharacterTransformationRepository transformationRepository;
    private final CharacterFormulaContextFactory contextFactory;

    /**
     * Выполняет операции "learn form" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param sourceFeatureId идентификатор source feature, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public KnownFormResponse learnForm(PlayerCharacter character, UUID monsterId, UUID sourceFeatureId, Integer level) {
        if (!flags.formsActive()) {
            throw new BadRequestException("Runtime форм отключён");
        }
        if (knownFormRepository.existsByCharacterIdAndMonsterId(character.getId(), monsterId)) {
            throw new BadRequestException("Форма уже известна персонажу");
        }
        FeatureAllowedMonsterFilter filter = filterForFeature(sourceFeatureId);
        FormulaContext ctx = contextFactory.build(character);
        MonsterEligibilityResult eligibility = eligibilityService.check(monsterId, filter, ctx);
        if (!eligibility.isEligible()) {
            throw new BadRequestException("Недопустимая форма: " + eligibility.getReason());
        }
        CharacterKnownForm form = knownFormRepository.save(CharacterKnownForm.builder()
                .characterId(character.getId())
                .monsterId(monsterId)
                .sourceFeatureId(sourceFeatureId)
                .learnedAtLevel(level)
                .approvedByDm(false)
                .build());
        return toResponse(form);
    }

    /**
     * Возвращает список для операции "list known forms" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<KnownFormResponse> listKnownForms(UUID characterId) {
        return knownFormRepository.findByCharacterId(characterId).stream().map(this::toResponse).toList();
    }

    /**
     * Выполняет операции "approve form" в рамках бизнес-логики домена.
     * @param formId идентификатор form, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public KnownFormResponse approveForm(UUID formId) {
        CharacterKnownForm form = knownFormRepository.findById(formId)
                .orElseThrow(() -> new BadRequestException("Форма не найдена"));
        form.setApprovedByDm(true);
        return toResponse(knownFormRepository.save(form));
    }

    /**
     * Выполняет операции "start transformation" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param sourceFeatureId идентификатор source feature, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public TransformationResponse startTransformation(PlayerCharacter character, UUID monsterId, UUID sourceFeatureId) {
        if (!flags.formsActive()) {
            throw new BadRequestException("Runtime форм отключён");
        }
        // one active transformation at a time
        transformationRepository.findByCharacterIdAndStatus(character.getId(), "active").forEach(t -> {
            t.setStatus("ended");
            transformationRepository.save(t);
        });
        CharacterTransformation transformation = transformationRepository.save(CharacterTransformation.builder()
                .characterId(character.getId())
                .monsterId(monsterId)
                .sourceFeatureId(sourceFeatureId)
                .status("active")
                .build());
        return toResponse(transformation);
    }

    /**
     * Выполняет операции "end transformation" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void endTransformation(UUID characterId) {
        transformationRepository.findByCharacterIdAndStatus(characterId, "active").forEach(t -> {
            t.setStatus("ended");
            transformationRepository.save(t);
        });
    }

    /**
     * Выполняет операции "current transformation" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public TransformationResponse currentTransformation(UUID characterId) {
        return transformationRepository.findByCharacterIdAndStatus(characterId, "active").stream()
                .findFirst().map(this::toResponse).orElse(null);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private FeatureAllowedMonsterFilter filterForFeature(UUID sourceFeatureId) {
        if (sourceFeatureId == null) {
            return null;
        }
        List<UUID> ruleIds = resolver.approvedEnabledRules(List.of(sourceFeatureId)).stream()
                .map(FeatureRule::getId).toList();
        if (ruleIds.isEmpty()) {
            return null;
        }
        return filterRepository.findByFeatureRuleIdIn(ruleIds).stream().findFirst().orElse(null);
    }

    private KnownFormResponse toResponse(CharacterKnownForm form) {
        return KnownFormResponse.builder()
                .id(form.getId()).monsterId(form.getMonsterId())
                .sourceFeatureId(form.getSourceFeatureId())
                .learnedAtLevel(form.getLearnedAtLevel())
                .approvedByDm(form.isApprovedByDm())
                .build();
    }

    private TransformationResponse toResponse(CharacterTransformation t) {
        return TransformationResponse.builder()
                .id(t.getId()).monsterId(t.getMonsterId()).sourceFeatureId(t.getSourceFeatureId())
                .activeEffectId(t.getActiveEffectId()).status(t.getStatus())
                .startedAt(t.getStartedAt()).expiresAt(t.getExpiresAt())
                .build();
    }
}
