package com.dnd.app.controller;

import com.dnd.app.dto.featurerule.CreateFeatureRuleIssueRequest;
import com.dnd.app.dto.featurerule.CreateFeatureRuleRequest;
import com.dnd.app.dto.featurerule.FeatureRuleDetailResponse;
import com.dnd.app.dto.featurerule.FeatureRuleIssueResponse;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateRequest;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateResponse;
import com.dnd.app.dto.featurerule.FeatureFormulaValidateRequest;
import com.dnd.app.dto.featurerule.FeatureFormulaValidationResponse;
import com.dnd.app.dto.featurerule.FeatureRuleMetadataResponse;
import com.dnd.app.dto.featurerule.FeatureRuleResponse;
import com.dnd.app.dto.featurerule.FeatureRuleRevisionResponse;
import com.dnd.app.dto.featurerule.FeatureRuleValidationResponse;
import com.dnd.app.dto.featurerule.ProblemFeatureSummaryResponse;
import com.dnd.app.dto.featurerule.RevisionActionRequest;
import com.dnd.app.dto.featurerule.RuleSourceResponse;
import com.dnd.app.dto.featurerule.RulesetResponse;
import com.dnd.app.dto.featurerule.UpdateFeatureRuleRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.FeatureFormulaService;
import com.dnd.app.service.FeatureRuleAdminService;
import com.dnd.app.service.FeatureRuleIssueService;
import com.dnd.app.service.FeatureRuleRevisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Rule Workbench admin API (Stage 1). Authoring, review lifecycle and issue tracking for class-feature
 * rules. Secured under {@code /api/admin/**} (ADMIN role) by SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Feature Rules", description = "Class-feature rules: authoring, review, issues")
public class FeatureRuleAdminController {

    private final FeatureRuleAdminService featureRuleAdminService;
    private final FeatureRuleIssueService featureRuleIssueService;
    private final FeatureRuleRevisionService featureRuleRevisionService;
    private final FeatureFormulaService featureFormulaService;
    private final Executor controllerTaskExecutor;

    private static String usernameOf(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }

    // ── Metadata & triage list ──────────────────────────────────────────────

    @GetMapping("/feature-rules/metadata")
    @Operation(summary = "Controlled vocabularies for the Rule Workbench (rule types, statuses, severities)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleMetadataResponse>>> metadata() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleAdminService.getMetadata())),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/features")
    @Operation(summary = "List class features that have rules or issues, with aggregate triage counts")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProblemFeatureSummaryResponse>>>> problemFeatures(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) String reviewStatus,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleAdminService.listProblemFeatures(
                                classId, level, ruleType, reviewStatus, severity, lang))),
                controllerTaskExecutor);
    }

    @GetMapping("/class-features/{featureId}/detail")
    @Operation(summary = "Full feature card: source description plus all rules and issues")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleDetailResponse>>> featureDetail(
            @PathVariable UUID featureId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleAdminService.getFeatureDetail(featureId, lang))),
                controllerTaskExecutor);
    }

    // ── Rules for a feature ─────────────────────────────────────────────────

    @GetMapping("/class-features/{featureId}/rules")
    @Operation(summary = "List the rules attached to a class feature")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatureRuleResponse>>>> featureRules(
            @PathVariable UUID featureId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleAdminService.getRulesForFeature(featureId))),
                controllerTaskExecutor);
    }

    @PostMapping("/class-features/{featureId}/rules")
    @Operation(summary = "Create a new draft rule for a class feature")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleResponse>>> createRule(
            @PathVariable UUID featureId,
            @Valid @RequestBody CreateFeatureRuleRequest request,
            Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleAdminService.createRule(featureId, request, username), "Правило создано")),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{id}")
    @Operation(summary = "Update an existing rule's editable fields (records a revision)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleResponse>>> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeatureRuleRequest request,
            Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleAdminService.updateRule(id, request, username), "Правило обновлено")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/{id}/approve")
    @Operation(summary = "Approve the current revision (fails on unresolved error issue or invalid type)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleResponse>>> approveRule(
            @PathVariable UUID id,
            @RequestBody(required = false) RevisionActionRequest request,
            Authentication authentication) {
        final String username = usernameOf(authentication);
        final String reason = request != null ? request.getChangeReason() : null;
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleAdminService.approve(id, reason, username), "Правило утверждено")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/{id}/disable")
    @Operation(summary = "Disable a rule so the runtime never executes it")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleResponse>>> disableRule(
            @PathVariable UUID id,
            Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleAdminService.disable(id, username), "Правило отключено")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/{id}/validate")
    @Operation(summary = "Validate a rule without changing its status")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleValidationResponse>>> validateRule(
            @PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleAdminService.validate(id))),
                controllerTaskExecutor);
    }

    // ── Revisions & scope (Stage 2) ─────────────────────────────────────────

    @GetMapping("/feature-rules/{id}/revisions")
    @Operation(summary = "Revision history of a rule (newest first)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatureRuleRevisionResponse>>>> revisions(
            @PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleRevisionService.listHistory(id))),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/{id}/create-draft")
    @Operation(summary = "Create a new editable draft revision from the approved one")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleResponse>>> createDraft(
            @PathVariable UUID id,
            @RequestBody(required = false) RevisionActionRequest request,
            Authentication authentication) {
        final String username = usernameOf(authentication);
        final String reason = request != null ? request.getChangeReason() : null;
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleAdminService.newDraftFromApproved(id, reason, username), "Черновик создан")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/{id}/rollback")
    @Operation(summary = "Roll back the rule to a prior revision (makes it the active approved one)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleResponse>>> rollback(
            @PathVariable UUID id,
            @RequestBody RevisionActionRequest request,
            Authentication authentication) {
        final String username = usernameOf(authentication);
        final UUID target = request != null ? request.getTargetRevisionId() : null;
        final String reason = request != null ? request.getChangeReason() : null;
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleAdminService.rollback(id, target, reason, username), "Откат выполнен")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/rulesets")
    @Operation(summary = "List game rulesets (editions) for scope selection")
    public CompletableFuture<ResponseEntity<ApiResponse<List<RulesetResponse>>>> rulesets() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleAdminService.listRulesets())),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/rule-sources")
    @Operation(summary = "List game sources (sourcebooks) for scope selection")
    public CompletableFuture<ResponseEntity<ApiResponse<List<RuleSourceResponse>>>> ruleSources() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleAdminService.listRuleSources())),
                controllerTaskExecutor);
    }

    // ── Issues ──────────────────────────────────────────────────────────────

    @GetMapping("/class-features/issues")
    @Operation(summary = "Global list of class-feature issues, filterable by severity/resolved/class")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatureRuleIssueResponse>>>> globalIssues(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(required = false) UUID classId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleIssueService.listGlobal(severity, resolved, classId, lang))),
                controllerTaskExecutor);
    }

    @GetMapping("/class-features/{featureId}/issues")
    @Operation(summary = "List the issues raised on a class feature")
    public CompletableFuture<ResponseEntity<ApiResponse<List<FeatureRuleIssueResponse>>>> featureIssues(
            @PathVariable UUID featureId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleIssueService.listForFeature(featureId, lang))),
                controllerTaskExecutor);
    }

    @PostMapping("/class-features/{featureId}/issues")
    @Operation(summary = "Raise an issue on a class feature or one of its rules")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleIssueResponse>>> createIssue(
            @PathVariable UUID featureId,
            @Valid @RequestBody CreateFeatureRuleIssueRequest request,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleIssueService.create(featureId, request, lang), "Проблема создана")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rule-issues/{id}/resolve")
    @Operation(summary = "Mark an issue as resolved by the current admin")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleIssueResponse>>> resolveIssue(
            @PathVariable UUID id,
            Authentication authentication,
            @RequestParam(defaultValue = "en") String lang) {
        final String username = authentication != null ? authentication.getName() : null;
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleIssueService.resolve(id, username, lang), "Проблема закрыта")),
                controllerTaskExecutor);
    }

    // ── Formulas (Stage 3) ──────────────────────────────────────────────────

    @PostMapping("/feature-formulas/validate")
    @Operation(summary = "Validate a DSL expression against a declared result type")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureFormulaValidationResponse>>> validateFormula(
            @Valid @RequestBody FeatureFormulaValidateRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureFormulaService.validate(request.getExpression(), request.getResultType()))),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-formulas/evaluate-preview")
    @Operation(summary = "Evaluate a DSL expression against an explicit preview context")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureFormulaEvaluateResponse>>> previewFormula(
            @Valid @RequestBody FeatureFormulaEvaluateRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureFormulaService.evaluatePreview(request))),
                controllerTaskExecutor);
    }
}
