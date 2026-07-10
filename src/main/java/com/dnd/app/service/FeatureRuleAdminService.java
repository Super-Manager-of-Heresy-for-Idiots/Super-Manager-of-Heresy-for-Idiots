package com.dnd.app.service;

import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleIssue;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FeatureRuleRevision;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.domain.featurerule.FeatureChoiceGroup;
import com.dnd.app.domain.featurerule.FeatureChoiceOption;
import com.dnd.app.domain.featurerule.FeatureLanguageGrant;
import com.dnd.app.domain.featurerule.FeatureProficiencyGrant;
import com.dnd.app.dto.featurerule.CodeLabel;
import com.dnd.app.dto.featurerule.CreateFeatureRuleRequest;
import com.dnd.app.dto.featurerule.FeatureChoiceSummary;
import com.dnd.app.dto.featurerule.FeatureGrantSummary;
import com.dnd.app.dto.featurerule.FeatureRuleDetailResponse;
import com.dnd.app.dto.featurerule.FeatureRuleMetadataResponse;
import com.dnd.app.dto.featurerule.FeatureRuleResponse;
import com.dnd.app.dto.featurerule.FeatureRuleValidationResponse;
import com.dnd.app.dto.featurerule.ProblemFeatureSummaryResponse;
import com.dnd.app.dto.featurerule.RuleSourceResponse;
import com.dnd.app.dto.featurerule.RulesetResponse;
import com.dnd.app.dto.featurerule.UpdateFeatureRuleRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureChoiceGroupRepository;
import com.dnd.app.repository.FeatureChoiceOptionRepository;
import com.dnd.app.repository.FeatureLanguageGrantRepository;
import com.dnd.app.repository.FeatureProficiencyGrantRepository;
import com.dnd.app.repository.FeatureRuleRevisionRepository;
import com.dnd.app.repository.RuleSourceRepository;
import com.dnd.app.repository.RulesetRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс FeatureRuleAdminService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureRuleAdminService {

    private static final String OWNER = FeatureRuleOwnerType.CLASS_FEATURE.getCode();
    private static final String ERROR = FeatureIssueSeverity.ERROR.getCode();

    /** Known issue types (mirrors the {@code rule_issue_type} seed / plan §4.2). */
    private static final List<String> ISSUE_TYPES = List.of(
            "ambiguous_parse", "missing_reference", "unsupported_formula",
            "unsupported_trigger", "requires_manual_choice", "conflicting_rules");

    private final FeatureRuleRepository ruleRepository;
    private final FeatureRuleIssueRepository issueRepository;
    private final ClassFeatureRepository classFeatureRepository;
    private final FeatureRuleIssueService featureRuleIssueService;
    private final FeatureRuleRevisionRepository revisionRepository;
    private final FeatureRuleRevisionService revisionService;
    private final FeatureRuleValidator validator;
    private final RulesetRepository rulesetRepository;
    private final RuleSourceRepository ruleSourceRepository;
    private final FeatureProficiencyGrantRepository proficiencyGrantRepository;
    private final FeatureLanguageGrantRepository languageGrantRepository;
    private final FeatureChoiceGroupRepository choiceGroupRepository;
    private final FeatureChoiceOptionRepository choiceOptionRepository;

    // ── Metadata ───────────────────────────────────────────────────────────

    /**
     * Возвращает результат операции "get metadata" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatureRuleMetadataResponse getMetadata() {
        List<CodeLabel> ruleTypes = new ArrayList<>();
        for (FeatureRuleProfile p : FeatureRuleProfile.values()) {
            ruleTypes.add(new CodeLabel(p.getCode(), p.getDescription()));
        }
        List<CodeLabel> statuses = new ArrayList<>();
        for (FeatureReviewStatus s : FeatureReviewStatus.values()) {
            statuses.add(new CodeLabel(s.getCode(), pretty(s.getCode())));
        }
        List<CodeLabel> severities = new ArrayList<>();
        for (FeatureIssueSeverity s : FeatureIssueSeverity.values()) {
            severities.add(new CodeLabel(s.getCode(), pretty(s.getCode())));
        }
        List<CodeLabel> sources = new ArrayList<>();
        for (FeatureRuleSource s : FeatureRuleSource.values()) {
            sources.add(new CodeLabel(s.getCode(), pretty(s.getCode())));
        }
        List<CodeLabel> issueTypes = ISSUE_TYPES.stream()
                .map(c -> new CodeLabel(c, pretty(c)))
                .toList();

        return FeatureRuleMetadataResponse.builder()
                .ruleTypes(ruleTypes)
                .reviewStatuses(statuses)
                .severities(severities)
                .issueTypes(issueTypes)
                .sources(sources)
                .build();
    }

    /**
     * Возвращает список для операции "list rulesets" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<RulesetResponse> listRulesets() {
        return rulesetRepository.findAllByOrderByEditionAsc().stream()
                .map(r -> RulesetResponse.builder()
                        .id(r.getId())
                        .key(r.getKey())
                        .displayName(r.getDisplayName())
                        .edition(r.getEdition())
                        .enabled(r.isEnabled())
                        .build())
                .toList();
    }

    /**
     * Возвращает список для операции "list rule sources" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<RuleSourceResponse> listRuleSources() {
        return ruleSourceRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(rs -> RuleSourceResponse.builder()
                        .id(rs.getId())
                        .key(rs.getKey())
                        .displayName(rs.getDisplayName())
                        .sourceType(rs.getSourceType())
                        .rulesetId(rs.getRulesetId())
                        .build())
                .toList();
    }

    // ── Rules for a feature ─────────────────────────────────────────────────

    /**
     * Возвращает результат операции "get rules for feature" в рамках бизнес-логики домена.
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<FeatureRuleResponse> getRulesForFeature(UUID featureId) {
        requireFeature(featureId);
        List<FeatureRule> rules =
                ruleRepository.findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER, featureId);
        List<FeatureRuleIssue> issues = issueRepository.findByOwnerTypeAndOwnerIdOrderByResolvedAscCreatedAtDesc(OWNER, featureId);
        return rules.stream().map(rule -> toResponse(rule, issues)).toList();
    }

    /**
     * Возвращает результат операции "get feature detail" в рамках бизнес-логики домена.
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatureRuleDetailResponse getFeatureDetail(UUID featureId, String lang) {
        ClassFeature feature = requireFeature(featureId);
        List<UUID> ruleIds = ruleRepository
                .findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER, featureId).stream()
                .map(FeatureRule::getId)
                .toList();
        return FeatureRuleDetailResponse.builder()
                .featureId(feature.getId())
                .slug(feature.getSlug())
                .title(feature.getTitle())
                .className(className(feature, lang))
                .subclassName(subclassName(feature, lang))
                .level(feature.getLevel())
                .description(feature.getDescription())
                .rules(getRulesForFeature(featureId))
                .issues(featureRuleIssueService.listForFeature(featureId, lang))
                .grants(buildGrantSummaries(ruleIds))
                .choices(buildChoiceSummaries(ruleIds))
                .build();
    }

    private List<FeatureGrantSummary> buildGrantSummaries(List<UUID> ruleIds) {
        if (ruleIds.isEmpty()) {
            return List.of();
        }
        List<FeatureGrantSummary> out = new ArrayList<>();
        for (FeatureProficiencyGrant g : proficiencyGrantRepository.findByFeatureRuleIdIn(ruleIds)) {
            out.add(FeatureGrantSummary.builder()
                    .id(g.getId())
                    .featureRuleId(g.getFeatureRuleId())
                    .kind("proficiency")
                    .proficiencyType(g.getProficiencyType())
                    .targetId(g.getTargetId())
                    .expertise(g.isExpertise())
                    .grantTiming(g.getGrantTiming())
                    .filterRuleId(g.getFilterRuleId())
                    .build());
        }
        for (FeatureLanguageGrant g : languageGrantRepository.findByFeatureRuleIdIn(ruleIds)) {
            out.add(FeatureGrantSummary.builder()
                    .id(g.getId())
                    .featureRuleId(g.getFeatureRuleId())
                    .kind("language")
                    .languageId(g.getLanguageId())
                    .grantTiming(g.getGrantTiming())
                    .filterRuleId(g.getFilterRuleId())
                    .build());
        }
        return out;
    }

    private List<FeatureChoiceSummary> buildChoiceSummaries(List<UUID> ruleIds) {
        if (ruleIds.isEmpty()) {
            return List.of();
        }
        List<FeatureChoiceGroup> groups = choiceGroupRepository.findByFeatureRuleIdIn(ruleIds);
        if (groups.isEmpty()) {
            return List.of();
        }
        List<UUID> groupIds = groups.stream().map(FeatureChoiceGroup::getId).toList();
        Map<UUID, List<FeatureChoiceOption>> optionsByGroup =
                choiceOptionRepository.findByChoiceGroupIdIn(groupIds).stream()
                        .collect(Collectors.groupingBy(FeatureChoiceOption::getChoiceGroupId));

        return groups.stream().map(group -> FeatureChoiceSummary.builder()
                        .id(group.getId())
                        .featureRuleId(group.getFeatureRuleId())
                        .choiceKey(group.getChoiceKey())
                        .minChoices(group.getMinChoices())
                        .maxChoicesFormulaId(group.getMaxChoicesFormulaId())
                        .choiceTiming(group.getChoiceTiming())
                        .replacePolicy(group.getReplacePolicy())
                        .options(optionsByGroup.getOrDefault(group.getId(), List.of()).stream()
                                .sorted(Comparator.comparing(FeatureChoiceOption::getSortOrder,
                                        Comparator.nullsLast(Integer::compareTo)))
                                .map(o -> FeatureChoiceSummary.Option.builder()
                                        .id(o.getId())
                                        .optionType(o.getOptionType())
                                        .targetEntityId(o.getTargetEntityId())
                                        .filterRuleId(o.getFilterRuleId())
                                        .sortOrder(o.getSortOrder())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    /**
     * Создает результат операции "create rule" в рамках бизнес-логики домена.
     * @param featureId идентификатор feature, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleResponse createRule(UUID featureId, CreateFeatureRuleRequest request, String actingUsername) {
        requireFeature(featureId);
        String ruleType = requireKnownRuleType(request.getRuleType());

        FeatureRule rule = FeatureRule.builder()
                .ownerType(OWNER)
                .ownerId(featureId)
                .ruleType(ruleType)
                .enabled(request.getEnabled() == null || request.getEnabled())
                .reviewStatus(FeatureReviewStatus.DRAFT.getCode())
                .source(FeatureRuleSource.MANUAL.getCode())
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .notes(trimToNull(request.getNotes()))
                .build();
        rule = ruleRepository.save(rule);
        revisionService.createInitialDraft(rule, actingUsername);
        return toResponse(rule, List.of());
    }

    /**
     * Обновляет результат операции "update rule" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleResponse updateRule(UUID ruleId, UpdateFeatureRuleRequest request, String actingUsername) {
        FeatureRule rule = requireRule(ruleId);
        if (request.getRuleType() != null) {
            rule.setRuleType(requireKnownRuleType(request.getRuleType()));
        }
        if (request.getEnabled() != null) {
            rule.setEnabled(request.getEnabled());
        }
        if (request.getSortOrder() != null) {
            rule.setSortOrder(request.getSortOrder());
        }
        if (request.getNotes() != null) {
            rule.setNotes(trimToNull(request.getNotes()));
        }
        if (request.getConfidence() != null) {
            rule.setConfidence(request.getConfidence());
        }
        if (request.getRulesetId() != null) {
            rule.setRulesetId(request.getRulesetId());
        }
        if (request.getRuleSourceId() != null) {
            rule.setRuleSourceId(request.getRuleSourceId());
        }
        if (request.getPriority() != null) {
            rule.setPriority(request.getPriority());
        }
        rule = ruleRepository.save(rule);
        revisionService.recordEdit(rule, actingUsername);
        return toResponse(rule, issuesForRuleOwner(rule));
    }

    // ── Review lifecycle (delegates snapshot/revision handling to the revision service) ──

    /**
     * Проверяет корректность операции "validate" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatureRuleValidationResponse validate(UUID ruleId) {
        return validator.validate(requireRule(ruleId));
    }

    /**
     * Выполняет операции "approve" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param changeReason входящее значение change reason, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleResponse approve(UUID ruleId, String changeReason, String actingUsername) {
        FeatureRule rule = revisionService.approveCurrent(ruleId, changeReason, actingUsername);
        return toResponse(rule, issuesForRuleOwner(rule));
    }

    /**
     * Выполняет операции "disable" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleResponse disable(UUID ruleId, String actingUsername) {
        FeatureRule rule = revisionService.disable(ruleId, actingUsername);
        return toResponse(rule, issuesForRuleOwner(rule));
    }

    /**
     * Выполняет операции "new draft from approved" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param changeReason входящее значение change reason, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleResponse newDraftFromApproved(UUID ruleId, String changeReason, String actingUsername) {
        FeatureRule rule = revisionService.newDraftFromApproved(ruleId, changeReason, actingUsername);
        return toResponse(rule, issuesForRuleOwner(rule));
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
    public FeatureRuleResponse rollback(UUID ruleId, UUID targetRevisionId, String changeReason, String actingUsername) {
        FeatureRule rule = revisionService.rollback(ruleId, targetRevisionId, changeReason, actingUsername);
        return toResponse(rule, issuesForRuleOwner(rule));
    }

    /**
     * Выполняет операции "batch approve low risk" в рамках бизнес-логики домена.
     * @param ruleType входящее значение rule type, используемое бизнес-сценарием
     * @param actingUsername имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int batchApproveLowRisk(String ruleType, String actingUsername) {
        java.util.Set<String> lowRisk = java.util.Set.of(
                FeatureRuleProfile.STATIC_GRANT.getCode());
        if (!lowRisk.contains(ruleType)) {
            throw new BadRequestException("Тип не входит в low-risk для массового утверждения: " + ruleType);
        }
        List<FeatureRule> candidates = ruleRepository.findByOwnerType(OWNER).stream()
                .filter(r -> ruleType.equals(r.getRuleType()))
                .filter(r -> FeatureReviewStatus.NEEDS_REVIEW.getCode().equals(r.getReviewStatus()))
                .filter(FeatureRule::isEnabled)
                .filter(r -> validator.validate(r).isValid())
                .toList();
        int approved = 0;
        for (FeatureRule rule : candidates) {
            revisionService.approveCurrent(rule.getId(), "batch approve (low-risk)", actingUsername);
            approved++;
        }
        return approved;
    }

    // ── Problem features list ───────────────────────────────────────────────

    /**
     * Возвращает список для операции "list problem features" в рамках бизнес-логики домена.
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param level входящее значение level, используемое бизнес-сценарием
     * @param ruleType входящее значение rule type, используемое бизнес-сценарием
     * @param reviewStatus входящее значение review status, используемое бизнес-сценарием
     * @param severity входящее значение severity, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ProblemFeatureSummaryResponse> listProblemFeatures(
            UUID classId, Integer level, String ruleType, String reviewStatus, String severity, String lang) {

        List<FeatureRule> allRules = ruleRepository.findByOwnerType(OWNER);
        List<FeatureRuleIssue> allIssues = issueRepository.findByOwnerType(OWNER);

        Map<UUID, List<FeatureRule>> rulesByFeature = allRules.stream()
                .collect(Collectors.groupingBy(FeatureRule::getOwnerId));
        Map<UUID, List<FeatureRuleIssue>> issuesByFeature = allIssues.stream()
                .collect(Collectors.groupingBy(FeatureRuleIssue::getOwnerId));

        // Every feature that has at least one rule or issue.
        List<UUID> featureIds = new ArrayList<>();
        featureIds.addAll(rulesByFeature.keySet());
        issuesByFeature.keySet().forEach(id -> {
            if (!rulesByFeature.containsKey(id)) {
                featureIds.add(id);
            }
        });
        if (featureIds.isEmpty()) {
            return List.of();
        }

        Map<UUID, ClassFeature> features = classFeatureRepository.findAllById(featureIds).stream()
                .collect(Collectors.toMap(ClassFeature::getId, f -> f));

        List<ProblemFeatureSummaryResponse> result = new ArrayList<>();
        for (UUID featureId : featureIds) {
            ClassFeature feature = features.get(featureId);
            if (feature == null) {
                continue; // orphaned rule/issue (owner deleted): skip
            }
            if (classId != null
                    && (feature.getCharacterClass() == null || !classId.equals(feature.getCharacterClass().getId()))) {
                continue;
            }
            if (level != null && !level.equals(feature.getLevel())) {
                continue;
            }
            List<FeatureRule> rules = rulesByFeature.getOrDefault(featureId, List.of());
            List<FeatureRuleIssue> issues = issuesByFeature.getOrDefault(featureId, List.of());

            if (ruleType != null && rules.stream().noneMatch(r -> ruleType.equals(r.getRuleType()))) {
                continue;
            }
            if (reviewStatus != null && rules.stream().noneMatch(r -> reviewStatus.equals(r.getReviewStatus()))) {
                continue;
            }
            if (severity != null && issues.stream().noneMatch(i -> !i.isResolved() && severity.equals(i.getSeverity()))) {
                continue;
            }
            result.add(toSummary(feature, rules, issues, lang));
        }

        result.sort(Comparator
                .comparing(ProblemFeatureSummaryResponse::isHasUnresolvedError).reversed()
                .thenComparing(ProblemFeatureSummaryResponse::getClassName, Comparator.nullsLast(String::compareTo))
                .thenComparing(r -> r.getLevel() == null ? Integer.MAX_VALUE : r.getLevel()));
        return result;
    }

    // ── Mapping helpers ─────────────────────────────────────────────────────

    private ProblemFeatureSummaryResponse toSummary(
            ClassFeature feature, List<FeatureRule> rules, List<FeatureRuleIssue> issues, String lang) {
        long approved = rules.stream()
                .filter(r -> FeatureReviewStatus.APPROVED.getCode().equals(r.getReviewStatus()))
                .count();
        List<FeatureRuleIssue> open = issues.stream().filter(i -> !i.isResolved()).toList();
        boolean hasError = open.stream().anyMatch(i -> ERROR.equals(i.getSeverity()));
        String maxSeverity = open.stream()
                .map(FeatureRuleIssue::getSeverity)
                .max(Comparator.comparingInt(FeatureRuleAdminService::severityRank))
                .orElse(null);

        return ProblemFeatureSummaryResponse.builder()
                .featureId(feature.getId())
                .slug(feature.getSlug())
                .title(feature.getTitle())
                .className(className(feature, lang))
                .subclassName(subclassName(feature, lang))
                .level(feature.getLevel())
                .ruleCount(rules.size())
                .approvedRuleCount(approved)
                .issueCount(issues.size())
                .openIssueCount(open.size())
                .hasUnresolvedError(hasError)
                .maxOpenSeverity(maxSeverity)
                .build();
    }

    private FeatureRuleResponse toResponse(FeatureRule rule, List<FeatureRuleIssue> featureIssues) {
        List<FeatureRuleIssue> ruleIssues = featureIssues.stream()
                .filter(i -> rule.getId().equals(i.getFeatureRuleId()) && !i.isResolved())
                .toList();
        boolean hasError = ruleIssues.stream().anyMatch(i -> ERROR.equals(i.getSeverity()));
        String label = FeatureRuleProfile.fromCode(rule.getRuleType())
                .map(FeatureRuleProfile::getDescription)
                .orElse(rule.getRuleType());

        List<FeatureRuleRevision> revisions =
                revisionRepository.findByFeatureRuleIdOrderByRevisionNumberDesc(rule.getId());
        Integer currentNumber = null;
        Integer approvedNumber = null;
        for (FeatureRuleRevision rev : revisions) {
            if (rev.getId().equals(rule.getCurrentRevisionId())) {
                currentNumber = rev.getRevisionNumber();
            }
            if (rev.getId().equals(rule.getApprovedRevisionId())) {
                approvedNumber = rev.getRevisionNumber();
            }
        }

        return FeatureRuleResponse.builder()
                .id(rule.getId())
                .ownerType(rule.getOwnerType())
                .ownerId(rule.getOwnerId())
                .ruleType(rule.getRuleType())
                .ruleTypeLabel(label)
                .enabled(rule.isEnabled())
                .reviewStatus(rule.getReviewStatus())
                .confidence(rule.getConfidence())
                .source(rule.getSource())
                .sortOrder(rule.getSortOrder())
                .notes(rule.getNotes())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .currentRevisionId(rule.getCurrentRevisionId())
                .approvedRevisionId(rule.getApprovedRevisionId())
                .currentRevisionNumber(currentNumber)
                .approvedRevisionNumber(approvedNumber)
                .revisionCount(revisions.size())
                .rulesetId(rule.getRulesetId())
                .ruleSourceId(rule.getRuleSourceId())
                .priority(rule.getPriority())
                .openIssueCount(ruleIssues.size())
                .hasUnresolvedError(hasError)
                .build();
    }

    // ── Small utilities ─────────────────────────────────────────────────────

    private List<FeatureRuleIssue> issuesForRuleOwner(FeatureRule rule) {
        return issueRepository.findByOwnerTypeAndOwnerIdOrderByResolvedAscCreatedAtDesc(OWNER, rule.getOwnerId());
    }

    private ClassFeature requireFeature(UUID featureId) {
        return classFeatureRepository.findById(featureId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение класса не найдено: " + featureId));
    }

    private FeatureRule requireRule(UUID ruleId) {
        return ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило умения не найдено: " + ruleId));
    }

    private static String requireKnownRuleType(String ruleType) {
        return FeatureRuleProfile.fromCode(ruleType)
                .map(FeatureRuleProfile::getCode)
                .orElseThrow(() -> new BadRequestException("Неизвестный тип правила: " + ruleType));
    }

    private String className(ClassFeature feature, String lang) {
        if (feature.getCharacterClass() == null) {
            return null;
        }
        return Localization.pick(lang,
                feature.getCharacterClass().getNameRu(),
                feature.getCharacterClass().getNameEn(),
                feature.getCharacterClass().getNameRu());
    }

    private String subclassName(ClassFeature feature, String lang) {
        if (feature.getSubclass() == null) {
            return null;
        }
        return Localization.pick(lang,
                feature.getSubclass().getNameRu(),
                feature.getSubclass().getNameEn(),
                feature.getSubclass().getNameRu());
    }

    private static int severityRank(String severity) {
        if (FeatureIssueSeverity.ERROR.getCode().equals(severity)) {
            return 3;
        }
        if (FeatureIssueSeverity.WARN.getCode().equals(severity)) {
            return 2;
        }
        if (FeatureIssueSeverity.INFO.getCode().equals(severity)) {
            return 1;
        }
        return 0;
    }

    private static String pretty(String code) {
        if (code == null || code.isBlank()) {
            return code;
        }
        String spaced = code.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
