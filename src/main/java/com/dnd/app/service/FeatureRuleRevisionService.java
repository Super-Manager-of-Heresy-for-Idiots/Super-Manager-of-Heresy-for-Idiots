package com.dnd.app.service;

import com.dnd.app.domain.User;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleRevision;
import com.dnd.app.dto.featurerule.FeatureRuleRevisionResponse;
import com.dnd.app.dto.featurerule.FeatureRuleValidationResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureRuleRevisionRepository;
import com.dnd.app.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс FeatureRuleRevisionService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureRuleRevisionService {

    private static final String DRAFT = FeatureReviewStatus.DRAFT.getCode();
    private static final String NEEDS_REVIEW = FeatureReviewStatus.NEEDS_REVIEW.getCode();
    private static final String APPROVED = FeatureReviewStatus.APPROVED.getCode();
    private static final String DISABLED = FeatureReviewStatus.DISABLED.getCode();

    private final FeatureRuleRepository ruleRepository;
    private final FeatureRuleRevisionRepository revisionRepository;
    private final FeatureRuleValidator validator;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Создает результат операции "create initial draft" в рамках бизнес-логики домена.
     * @param rule входящее значение rule, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void createInitialDraft(FeatureRule rule, String actingUsername) {
        FeatureRuleRevision rev = createDraft(rule, snapshot(rule), "initial", resolveUserId(actingUsername));
        rule.setCurrentRevisionId(rev.getId());
        ruleRepository.save(rule);
    }

    /**
     * Выполняет операции "record edit" в рамках бизнес-логики домена.
     * @param rule входящее значение rule, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void recordEdit(FeatureRule rule, String actingUsername) {
        FeatureRuleRevision current = currentRevision(rule);
        if (current != null && (DRAFT.equals(current.getStatus()) || NEEDS_REVIEW.equals(current.getStatus()))) {
            current.setRulePayloadSnapshot(snapshot(rule));
            revisionRepository.save(current);
            return;
        }
        FeatureRuleRevision draft = createDraft(rule, snapshot(rule), null, resolveUserId(actingUsername));
        rule.setCurrentRevisionId(draft.getId());
        rule.setReviewStatus(DRAFT);
        ruleRepository.save(rule);
    }

    /**
     * Выполняет операции "approve current" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param changeReason входящее значение change reason, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRule approveCurrent(UUID ruleId, String changeReason, String actingUsername) {
        FeatureRule rule = requireRule(ruleId);
        FeatureRuleValidationResponse validation = validator.validate(rule);
        if (!validation.isValid()) {
            throw new BadRequestException("Нельзя утвердить правило: " + String.join("; ", validation.getProblems()));
        }
        UUID userId = resolveUserId(actingUsername);
        FeatureRuleRevision current = currentRevision(rule);
        if (current == null) {
            current = createDraft(rule, snapshot(rule), changeReason, userId);
            rule.setCurrentRevisionId(current.getId());
        }
        current.setStatus(APPROVED);
        current.setApprovedBy(userId);
        current.setApprovedAt(Instant.now());
        if (changeReason != null && !changeReason.isBlank()) {
            current.setChangeReason(changeReason.trim());
        }
        revisionRepository.save(current);

        rule.setApprovedRevisionId(current.getId());
        rule.setReviewStatus(APPROVED);
        return ruleRepository.save(rule);
    }

    /**
     * Выполняет операции "disable" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRule disable(UUID ruleId, String actingUsername) {
        FeatureRule rule = requireRule(ruleId);
        FeatureRuleRevision current = currentRevision(rule);
        if (current != null) {
            current.setStatus(DISABLED);
            current.setDisabledBy(resolveUserId(actingUsername));
            current.setDisabledAt(Instant.now());
            revisionRepository.save(current);
        }
        rule.setReviewStatus(DISABLED);
        return ruleRepository.save(rule);
    }

    /**
     * Выполняет операции "new draft from approved" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param changeReason входящее значение change reason, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRule newDraftFromApproved(UUID ruleId, String changeReason, String actingUsername) {
        FeatureRule rule = requireRule(ruleId);
        if (rule.getApprovedRevisionId() == null) {
            throw new BadRequestException("У правила нет утверждённой ревизии для создания черновика");
        }
        FeatureRuleRevision approved = revisionRepository.findById(rule.getApprovedRevisionId())
                .orElseThrow(() -> new ResourceNotFoundException("Утверждённая ревизия не найдена"));
        FeatureRuleRevision draft = createDraft(
                rule, approved.getRulePayloadSnapshot(), changeReason, resolveUserId(actingUsername));
        rule.setCurrentRevisionId(draft.getId());
        rule.setReviewStatus(DRAFT);
        return ruleRepository.save(rule);
    }

    /**
     * Выполняет бросок операции "rollback" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param targetRevisionId идентификатор target revision, используемый для выбора нужного бизнес-объекта
     * @param changeReason входящее значение change reason, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRule rollback(UUID ruleId, UUID targetRevisionId, String changeReason, String actingUsername) {
        FeatureRule rule = requireRule(ruleId);
        if (targetRevisionId == null) {
            throw new BadRequestException("Не указана ревизия для отката");
        }
        FeatureRuleRevision target = revisionRepository.findById(targetRevisionId)
                .orElseThrow(() -> new ResourceNotFoundException("Ревизия не найдена: " + targetRevisionId));
        if (!rule.getId().equals(target.getFeatureRuleId())) {
            throw new BadRequestException("Ревизия не принадлежит этому правилу");
        }
        target.setStatus(APPROVED);
        target.setApprovedBy(resolveUserId(actingUsername));
        target.setApprovedAt(Instant.now());
        if (changeReason != null && !changeReason.isBlank()) {
            target.setChangeReason(changeReason.trim());
        }
        revisionRepository.save(target);

        rule.setApprovedRevisionId(target.getId());
        rule.setCurrentRevisionId(target.getId());
        rule.setReviewStatus(APPROVED);
        return ruleRepository.save(rule);
    }

    /**
     * Возвращает список для операции "list history" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatureRuleRevisionResponse> listHistory(UUID ruleId) {
        FeatureRule rule = requireRule(ruleId);
        return revisionRepository.findByFeatureRuleIdOrderByRevisionNumberDesc(ruleId).stream()
                .map(rev -> toResponse(rev, rule))
                .toList();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * Преобразует данные операции "to response" в рамках бизнес-логики домена.
     * @param rev входящее значение rev, используемое бизнес-сценарием
     * @param rule входящее значение rule, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public FeatureRuleRevisionResponse toResponse(FeatureRuleRevision rev, FeatureRule rule) {
        return FeatureRuleRevisionResponse.builder()
                .id(rev.getId())
                .featureRuleId(rev.getFeatureRuleId())
                .revisionNumber(rev.getRevisionNumber())
                .status(rev.getStatus())
                .rulePayloadSnapshot(rev.getRulePayloadSnapshot())
                .changeReason(rev.getChangeReason())
                .createdBy(rev.getCreatedBy())
                .createdAt(rev.getCreatedAt())
                .approvedBy(rev.getApprovedBy())
                .approvedAt(rev.getApprovedAt())
                .disabledBy(rev.getDisabledBy())
                .disabledAt(rev.getDisabledAt())
                .current(rev.getId().equals(rule.getCurrentRevisionId()))
                .approvedActive(rev.getId().equals(rule.getApprovedRevisionId()))
                .build();
    }

    private FeatureRuleRevision createDraft(FeatureRule rule, String snapshot, String reason, UUID userId) {
        FeatureRuleRevision rev = FeatureRuleRevision.builder()
                .featureRuleId(rule.getId())
                .revisionNumber(nextRevisionNumber(rule.getId()))
                .status(DRAFT)
                .rulePayloadSnapshot(snapshot)
                .changeReason(reason)
                .createdBy(userId)
                .build();
        return revisionRepository.save(rev);
    }

    private int nextRevisionNumber(UUID ruleId) {
        return revisionRepository.findTopByFeatureRuleIdOrderByRevisionNumberDesc(ruleId)
                .map(r -> r.getRevisionNumber() + 1)
                .orElse(1);
    }

    private FeatureRuleRevision currentRevision(FeatureRule rule) {
        if (rule.getCurrentRevisionId() == null) {
            return null;
        }
        return revisionRepository.findById(rule.getCurrentRevisionId()).orElse(null);
    }

    private String snapshot(FeatureRule rule) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("ruleType", rule.getRuleType());
        m.put("enabled", rule.isEnabled());
        m.put("notes", rule.getNotes());
        m.put("sortOrder", rule.getSortOrder());
        m.put("priority", rule.getPriority());
        m.put("rulesetId", rule.getRulesetId());
        m.put("ruleSourceId", rule.getRuleSourceId());
        m.put("homebrewPackId", rule.getHomebrewPackId());
        m.put("campaignId", rule.getCampaignId());
        try {
            return objectMapper.writeValueAsString(m);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Не удалось сериализовать snapshot правила", e);
        }
    }

    private FeatureRule requireRule(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило умения не найдено: " + ruleId));
    }

    private UUID resolveUserId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepository.findByUsername(username).map(User::getId).orElse(null);
    }
}
