package com.dnd.app.service;

import com.dnd.app.domain.User;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleIssue;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.dto.featurerule.CreateFeatureRuleIssueRequest;
import com.dnd.app.dto.featurerule.FeatureRuleIssueResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс FeatureRuleIssueService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureRuleIssueService {

    private static final String OWNER = FeatureRuleOwnerType.CLASS_FEATURE.getCode();

    private final FeatureRuleIssueRepository issueRepository;
    private final FeatureRuleRepository ruleRepository;
    private final ClassFeatureRepository classFeatureRepository;
    private final UserRepository userRepository;

    /**
     * Возвращает список для операции "list for feature" в рамках бизнес-логики домена.
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatureRuleIssueResponse> listForFeature(UUID featureId, String lang) {
        ClassFeature feature = classFeatureRepository.findById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение класса не найдено: " + featureId));
        return issueRepository.findByOwnerTypeAndOwnerIdOrderByResolvedAscCreatedAtDesc(OWNER, featureId).stream()
                .map(issue -> toResponse(issue, feature, lang))
                .toList();
    }

    /**
     * Возвращает список для операции "list global" в рамках бизнес-логики домена.
     * @param severity входящее значение severity, используемое бизнес-сценарием
     * @param resolved входящее значение resolved, используемое бизнес-сценарием
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatureRuleIssueResponse> listGlobal(String severity, Boolean resolved, UUID classId, String lang) {
        List<FeatureRuleIssue> issues = issueRepository.findByOwnerType(OWNER).stream()
                .filter(i -> severity == null || severity.equals(i.getSeverity()))
                .filter(i -> resolved == null || resolved == i.isResolved())
                .toList();
        if (issues.isEmpty()) {
            return List.of();
        }
        List<UUID> featureIds = issues.stream().map(FeatureRuleIssue::getOwnerId).distinct().toList();
        Map<UUID, ClassFeature> features = classFeatureRepository.findAllById(featureIds).stream()
                .collect(Collectors.toMap(ClassFeature::getId, f -> f));

        return issues.stream()
                .map(i -> {
                    ClassFeature feature = features.get(i.getOwnerId());
                    return feature == null ? null : toResponseWithClassFilter(i, feature, classId, lang);
                })
                .filter(r -> r != null)
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();
    }

    /**
     * Создает результат операции "create" в рамках бизнес-логики домена.
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleIssueResponse create(UUID featureId, CreateFeatureRuleIssueRequest request, String lang) {
        ClassFeature feature = classFeatureRepository.findById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение класса не найдено: " + featureId));

        String severity = FeatureIssueSeverity.fromCode(request.getSeverity())
                .map(FeatureIssueSeverity::getCode)
                .orElseThrow(() -> new BadRequestException("Недопустимый severity: " + request.getSeverity()));

        if (request.getFeatureRuleId() != null) {
            FeatureRule rule = ruleRepository.findById(request.getFeatureRuleId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Правило умения не найдено: " + request.getFeatureRuleId()));
            if (!featureId.equals(rule.getOwnerId())) {
                throw new BadRequestException("Правило не принадлежит указанному умению");
            }
        }

        FeatureRuleIssue issue = FeatureRuleIssue.builder()
                .ownerType(OWNER)
                .ownerId(featureId)
                .featureRuleId(request.getFeatureRuleId())
                .issueType(request.getIssueType().trim())
                .severity(severity)
                .message(request.getMessage().trim())
                .sourceTextFragment(trimToNull(request.getSourceTextFragment()))
                .resolved(false)
                .build();
        issue = issueRepository.save(issue);
        return toResponse(issue, feature, lang);
    }

    /**
     * Выполняет операции "resolve" в рамках бизнес-логики домена.
     * @param issueId идентификатор issue, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleIssueResponse resolve(UUID issueId, String username, String lang) {
        FeatureRuleIssue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Проблема не найдена: " + issueId));
        if (!issue.isResolved()) {
            issue.setResolved(true);
            issue.setResolvedAt(Instant.now());
            issue.setResolvedBy(resolveUserId(username));
            issue = issueRepository.save(issue);
        }
        ClassFeature feature = classFeatureRepository.findById(issue.getOwnerId()).orElse(null);
        return toResponse(issue, feature, lang);
    }

    // ── Mapping ─────────────────────────────────────────────────────────────

    private FeatureRuleIssueResponse toResponseWithClassFilter(
            FeatureRuleIssue issue, ClassFeature feature, UUID classId, String lang) {
        if (classId != null
                && (feature.getCharacterClass() == null || !classId.equals(feature.getCharacterClass().getId()))) {
            return null;
        }
        return toResponse(issue, feature, lang);
    }

    private FeatureRuleIssueResponse toResponse(FeatureRuleIssue issue, ClassFeature feature, String lang) {
        String className = null;
        String subclassName = null;
        String title = null;
        Integer level = null;
        if (feature != null) {
            title = feature.getTitle();
            level = feature.getLevel();
            if (feature.getCharacterClass() != null) {
                className = Localization.pick(lang,
                        feature.getCharacterClass().getNameRu(),
                        feature.getCharacterClass().getNameEn(),
                        feature.getCharacterClass().getNameRu());
            }
            if (feature.getSubclass() != null) {
                subclassName = Localization.pick(lang,
                        feature.getSubclass().getNameRu(),
                        feature.getSubclass().getNameEn(),
                        feature.getSubclass().getNameRu());
            }
        }
        return FeatureRuleIssueResponse.builder()
                .id(issue.getId())
                .ownerType(issue.getOwnerType())
                .ownerId(issue.getOwnerId())
                .featureRuleId(issue.getFeatureRuleId())
                .issueType(issue.getIssueType())
                .severity(issue.getSeverity())
                .message(issue.getMessage())
                .sourceTextFragment(issue.getSourceTextFragment())
                .resolved(issue.isResolved())
                .resolvedBy(issue.getResolvedBy())
                .resolvedAt(issue.getResolvedAt())
                .createdAt(issue.getCreatedAt())
                .updatedAt(issue.getUpdatedAt())
                .featureTitle(title)
                .className(className)
                .subclassName(subclassName)
                .level(level)
                .build();
    }

    private UUID resolveUserId(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        return userRepository.findByUsername(username).map(User::getId).orElse(null);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
