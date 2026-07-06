package com.dnd.app.controller;

import com.dnd.app.dto.featurerule.CreateFeatureRuleIssueRequest;
import com.dnd.app.dto.featurerule.CreateFeatureRuleRequest;
import com.dnd.app.dto.featurerule.FeatureRuleDetailResponse;
import com.dnd.app.dto.featurerule.FeatureRuleIssueResponse;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateRequest;
import com.dnd.app.dto.featurerule.FeatureFormulaEvaluateResponse;
import com.dnd.app.dto.featurerule.FeatureFormulaValidateRequest;
import com.dnd.app.dto.featurerule.FeatureFormulaValidationResponse;
import com.dnd.app.dto.featurerule.FeatureMaintenanceResult;
import com.dnd.app.dto.featurerule.FeatureRuleBackfillResult;
import com.dnd.app.dto.featurerule.SpellRuleBackfillResult;
import com.dnd.app.dto.featurerule.FeatureRuleCoverageReport;
import com.dnd.app.dto.featurerule.FeatureRuleMetadataResponse;
import com.dnd.app.dto.featurerule.FeatureRuleResponse;
import com.dnd.app.dto.featurerule.FeatureRuleRevisionResponse;
import com.dnd.app.dto.featurerule.FeatureRuleValidationResponse;
import com.dnd.app.dto.featurerule.ProblemFeatureSummaryResponse;
import com.dnd.app.dto.featurerule.ActionCostAdminResponse;
import com.dnd.app.dto.featurerule.ActionCostEditRequest;
import com.dnd.app.dto.featurerule.ActionTypeOption;
import com.dnd.app.dto.featurerule.DamageRuleAdminResponse;
import com.dnd.app.dto.featurerule.DamageRuleEditRequest;
import com.dnd.app.dto.featurerule.HealingRuleAdminResponse;
import com.dnd.app.dto.featurerule.HealingRuleEditRequest;
import com.dnd.app.dto.featurerule.ActiveEffectAdminResponse;
import com.dnd.app.dto.featurerule.ActiveEffectEditRequest;
import com.dnd.app.dto.featurerule.EffectMetadataResponse;
import com.dnd.app.dto.featurerule.ResolutionMetadataResponse;
import com.dnd.app.dto.featurerule.ResolutionRuleAdminResponse;
import com.dnd.app.dto.featurerule.ResolutionRuleEditRequest;
import com.dnd.app.dto.featurerule.MonsterFormAdminResponse;
import com.dnd.app.dto.featurerule.MonsterFormEditRequest;
import com.dnd.app.dto.featurerule.TriggerAdminResponse;
import com.dnd.app.dto.featurerule.TriggerEditRequest;
import com.dnd.app.dto.featurerule.SpellGrantAdminResponse;
import com.dnd.app.dto.featurerule.SpellGrantEditRequest;
import com.dnd.app.dto.featurerule.StaticGrantAdminResponse;
import com.dnd.app.dto.featurerule.StaticGrantEditRequest;
import com.dnd.app.dto.featurerule.ChoiceRuleAdminResponse;
import com.dnd.app.dto.featurerule.ChoiceRuleEditRequest;
import com.dnd.app.dto.featurerule.GenericFormulaRuleAdminResponse;
import com.dnd.app.dto.featurerule.GenericFormulaRuleEditRequest;
import com.dnd.app.dto.featurerule.CompanionDefinitionAdminResponse;
import com.dnd.app.dto.featurerule.CompanionDefinitionEditRequest;
import com.dnd.app.dto.featurerule.TargetTypeOption;
import com.dnd.app.dto.featurerule.ResourceDefinitionAdminResponse;
import com.dnd.app.dto.featurerule.ResourceDefinitionEditRequest;
import com.dnd.app.dto.featurerule.RevisionActionRequest;
import com.dnd.app.dto.featurerule.RuleSourceResponse;
import com.dnd.app.dto.featurerule.RulesetResponse;
import com.dnd.app.dto.featurerule.UpdateFeatureRuleRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.featurerule.FormulaVocabularyResponse;
import com.dnd.app.service.FeatureFormulaService;
import com.dnd.app.service.FeatureFormulaVocabularyService;
import com.dnd.app.service.FeatureRuleAdminService;
import com.dnd.app.service.BackgroundProficiencyBackfillService;
import com.dnd.app.service.FeatureRuleBackfillService;
import com.dnd.app.service.SpellRuleBackfillService;
import com.dnd.app.service.FeatureRuleCoverageService;
import com.dnd.app.service.FeatureActionCostAdminService;
import com.dnd.app.service.FeatureDamageRuleAdminService;
import com.dnd.app.service.FeatureHealingRuleAdminService;
import com.dnd.app.service.FeatureActiveEffectAdminService;
import com.dnd.app.service.FeatureResolutionRuleAdminService;
import com.dnd.app.service.FeatureFormsRuleAdminService;
import com.dnd.app.service.FeatureStaticGrantAdminService;
import com.dnd.app.service.FeatureChoiceRuleAdminService;
import com.dnd.app.service.FeatureGenericFormulaRuleAdminService;
import com.dnd.app.service.FeatureCompanionDefinitionAdminService;
import com.dnd.app.service.FeatureResourceDefinitionAdminService;
import com.dnd.app.service.FeatureRuleIssueService;
import com.dnd.app.service.FeatureRuleRevisionService;
import com.dnd.app.service.FeatureRuntimeMaintenanceService;
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
    private final FeatureFormulaVocabularyService featureFormulaVocabularyService;
    private final FeatureRuleBackfillService featureRuleBackfillService;
    private final FeatureRuleCoverageService featureRuleCoverageService;
    private final BackgroundProficiencyBackfillService backgroundProficiencyBackfillService;
    private final SpellRuleBackfillService spellRuleBackfillService;
    private final FeatureResourceDefinitionAdminService featureResourceDefinitionAdminService;
    private final FeatureDamageRuleAdminService featureDamageRuleAdminService;
    private final FeatureActionCostAdminService featureActionCostAdminService;
    private final FeatureHealingRuleAdminService featureHealingRuleAdminService;
    private final FeatureActiveEffectAdminService featureActiveEffectAdminService;
    private final FeatureResolutionRuleAdminService featureResolutionRuleAdminService;
    private final FeatureFormsRuleAdminService featureFormsRuleAdminService;
    private final FeatureStaticGrantAdminService featureStaticGrantAdminService;
    private final FeatureChoiceRuleAdminService featureChoiceRuleAdminService;
    private final FeatureGenericFormulaRuleAdminService featureGenericFormulaRuleAdminService;
    private final FeatureCompanionDefinitionAdminService featureCompanionDefinitionAdminService;
    private final FeatureRuntimeMaintenanceService featureRuntimeMaintenanceService;
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

    @GetMapping("/feature-rules/resource-keys")
    @Operation(summary = "List distinct resource keys already defined (for workbench autocomplete)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<String>>>> resourceKeys() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureResourceDefinitionAdminService.listResourceKeys())),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/resource-definition")
    @Operation(summary = "Get a RESOURCE rule's definition (resource key, max formula, reset) for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<ResourceDefinitionAdminResponse>>> getResourceDefinition(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureResourceDefinitionAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/resource-definition")
    @Operation(summary = "Create/update a RESOURCE rule's definition (key, max formula, reset window)")
    public CompletableFuture<ResponseEntity<ApiResponse<ResourceDefinitionAdminResponse>>> upsertResourceDefinition(
            @PathVariable UUID ruleId, @Valid @RequestBody ResourceDefinitionEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureResourceDefinitionAdminService.upsert(ruleId, request), "Ресурс сохранён")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/damage-rule")
    @Operation(summary = "Get a DAMAGE rule's definition (dice/flat formula, type, gating) for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<DamageRuleAdminResponse>>> getDamageRule(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureDamageRuleAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/damage-rule")
    @Operation(summary = "Create/update a DAMAGE rule (dice/flat formula, damage type, attack/save gating)")
    public CompletableFuture<ResponseEntity<ApiResponse<DamageRuleAdminResponse>>> upsertDamageRule(
            @PathVariable UUID ruleId, @Valid @RequestBody DamageRuleEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureDamageRuleAdminService.upsert(ruleId, request), "Урон сохранён")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/action-types")
    @Operation(summary = "List action-economy types (for the action-cost editor)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ActionTypeOption>>>> actionTypes() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureActionCostAdminService.listActionTypes())),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/action-cost")
    @Operation(summary = "Get an ACTION_COST rule's definition for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<ActionCostAdminResponse>>> getActionCost(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureActionCostAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/action-cost")
    @Operation(summary = "Create/update an ACTION_COST rule (action type, amount, condition)")
    public CompletableFuture<ResponseEntity<ApiResponse<ActionCostAdminResponse>>> upsertActionCost(
            @PathVariable UUID ruleId, @Valid @RequestBody ActionCostEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureActionCostAdminService.upsert(ruleId, request), "Стоимость действия сохранена")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/target-types")
    @Operation(summary = "List rule-target types (for the healing editor)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<TargetTypeOption>>>> targetTypes() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureHealingRuleAdminService.listTargetTypes())),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/healing-rule")
    @Operation(summary = "Get a HEALING rule's definition (amount formula, target, temp-HP/revive) for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<HealingRuleAdminResponse>>> getHealingRule(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureHealingRuleAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/healing-rule")
    @Operation(summary = "Create/update a HEALING rule (amount formula, target, temp-HP / revive flags)")
    public CompletableFuture<ResponseEntity<ApiResponse<HealingRuleAdminResponse>>> upsertHealingRule(
            @PathVariable UUID ruleId, @Valid @RequestBody HealingRuleEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureHealingRuleAdminService.upsert(ruleId, request), "Лечение сохранено")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/effect-metadata")
    @Operation(summary = "Reference vocabularies for the active-effect editor (durations, stacking, targets, triggers)")
    public CompletableFuture<ResponseEntity<ApiResponse<EffectMetadataResponse>>> effectMetadata() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureActiveEffectAdminService.metadata())),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/active-effect")
    @Operation(summary = "Get an ACTIVE_EFFECT rule's graph (definition + modifiers + end conditions) for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<ActiveEffectAdminResponse>>> getActiveEffect(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureActiveEffectAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/active-effect")
    @Operation(summary = "Create/update an ACTIVE_EFFECT rule (definition, stat modifiers, end conditions)")
    public CompletableFuture<ResponseEntity<ApiResponse<ActiveEffectAdminResponse>>> upsertActiveEffect(
            @PathVariable UUID ruleId, @Valid @RequestBody ActiveEffectEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureActiveEffectAdminService.upsert(ruleId, request), "Эффект сохранён")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/resolution-metadata")
    @Operation(summary = "Reference vocabularies for the save/check/attack editor (types, abilities, skills)")
    public CompletableFuture<ResponseEntity<ApiResponse<ResolutionMetadataResponse>>> resolutionMetadata() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureResolutionRuleAdminService.metadata())),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/resolution-rule")
    @Operation(summary = "Get a SAVE_CHECK_ATTACK rule's definition (type, ability/skill, DC) for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<ResolutionRuleAdminResponse>>> getResolutionRule(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureResolutionRuleAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/resolution-rule")
    @Operation(summary = "Create/update a SAVE_CHECK_ATTACK rule (resolution type, ability/skill, DC formula)")
    public CompletableFuture<ResponseEntity<ApiResponse<ResolutionRuleAdminResponse>>> upsertResolutionRule(
            @PathVariable UUID ruleId, @Valid @RequestBody ResolutionRuleEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureResolutionRuleAdminService.upsert(ruleId, request), "Проверка сохранена")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/monster-form")
    @Operation(summary = "Get a MONSTER_FORM (Wild Shape) filter for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterFormAdminResponse>>> getMonsterForm(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureFormsRuleAdminService.getMonsterForm(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/monster-form")
    @Operation(summary = "Create/update a MONSTER_FORM filter (creature type, max CR, movement/size/source)")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterFormAdminResponse>>> upsertMonsterForm(
            @PathVariable UUID ruleId, @Valid @RequestBody MonsterFormEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureFormsRuleAdminService.upsertMonsterForm(ruleId, request), "Форма сохранена")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/trigger")
    @Operation(summary = "Get a TRIGGER_REACTION binding for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<TriggerAdminResponse>>> getTrigger(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureFormsRuleAdminService.getTrigger(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/trigger")
    @Operation(summary = "Create/update a TRIGGER_REACTION rule (event, timing, predicate, reaction flags)")
    public CompletableFuture<ResponseEntity<ApiResponse<TriggerAdminResponse>>> upsertTrigger(
            @PathVariable UUID ruleId, @Valid @RequestBody TriggerEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureFormsRuleAdminService.upsertTrigger(ruleId, request), "Триггер сохранён")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/spell-grant")
    @Operation(summary = "Get a SPELL_GRANT for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellGrantAdminResponse>>> getSpellGrant(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureFormsRuleAdminService.getSpellGrant(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/spell-grant")
    @Operation(summary = "Create/update a SPELL_GRANT (spell + prepared/known/free-cast + ability override)")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellGrantAdminResponse>>> upsertSpellGrant(
            @PathVariable UUID ruleId, @Valid @RequestBody SpellGrantEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureFormsRuleAdminService.upsertSpellGrant(ruleId, request), "Заклинание сохранено")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/static-grants")
    @Operation(summary = "Get STATIC_GRANT rule details for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<StaticGrantAdminResponse>>> getStaticGrants(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureStaticGrantAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/static-grants")
    @Operation(summary = "Replace STATIC_GRANT proficiency/language grants")
    public CompletableFuture<ResponseEntity<ApiResponse<StaticGrantAdminResponse>>> replaceStaticGrants(
            @PathVariable UUID ruleId, @Valid @RequestBody StaticGrantEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureStaticGrantAdminService.replace(ruleId, request), "Static grants saved")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/choice-groups")
    @Operation(summary = "Get CHOICE rule groups/options for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<ChoiceRuleAdminResponse>>> getChoiceGroups(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureChoiceRuleAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/choice-groups")
    @Operation(summary = "Replace CHOICE rule groups/options")
    public CompletableFuture<ResponseEntity<ApiResponse<ChoiceRuleAdminResponse>>> replaceChoiceGroups(
            @PathVariable UUID ruleId, @Valid @RequestBody ChoiceRuleEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureChoiceRuleAdminService.replace(ruleId, request), "Choices saved")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/generic-formulas")
    @Operation(summary = "Get FORMULA/manual helper formulas for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<GenericFormulaRuleAdminResponse>>> getGenericFormulas(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureGenericFormulaRuleAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/generic-formulas")
    @Operation(summary = "Replace FORMULA/manual helper formula rows")
    public CompletableFuture<ResponseEntity<ApiResponse<GenericFormulaRuleAdminResponse>>> replaceGenericFormulas(
            @PathVariable UUID ruleId, @Valid @RequestBody GenericFormulaRuleEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureGenericFormulaRuleAdminService.replace(ruleId, request), "Formulas saved")),
                controllerTaskExecutor);
    }

    @GetMapping("/feature-rules/{ruleId}/companion-definitions")
    @Operation(summary = "Get COMPANION rule definitions for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<CompanionDefinitionAdminResponse>>> getCompanionDefinitions(
            @PathVariable UUID ruleId) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureCompanionDefinitionAdminService.get(ruleId))),
                controllerTaskExecutor);
    }

    @PutMapping("/feature-rules/{ruleId}/companion-definitions")
    @Operation(summary = "Replace COMPANION rule definitions")
    public CompletableFuture<ResponseEntity<ApiResponse<CompanionDefinitionAdminResponse>>> replaceCompanionDefinitions(
            @PathVariable UUID ruleId, @Valid @RequestBody CompanionDefinitionEditRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureCompanionDefinitionAdminService.replace(ruleId, request), "Companion definitions saved")),
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

    // ── Backfill, coverage & bulk review (Stage 12) ─────────────────────────

    @GetMapping("/feature-rules/coverage")
    @Operation(summary = "Coverage of the runtime features by the feature-rules model")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleCoverageReport>>> coverage(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleCoverageService.report(lang))),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/backfill")
    @Operation(summary = "Backfill structured rules for the 305 runtime features (dry-run unless apply=true)")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureRuleBackfillResult>>> backfill(
            @RequestParam(defaultValue = "false") boolean apply) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuleBackfillService.backfill(apply),
                                apply ? "Бэкфилл применён" : "Пробный прогон")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/backfill-backgrounds")
    @Operation(summary = "Backfill static skill-grant rules for backgrounds (S1; dry-run unless apply=true)")
    public CompletableFuture<ResponseEntity<ApiResponse<Integer>>> backfillBackgrounds(
            @RequestParam(defaultValue = "false") boolean apply) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(backgroundProficiencyBackfillService.backfill(apply),
                                apply ? "Бэкфилл предысторий применён" : "Пробный прогон")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/backfill-spells")
    @Operation(summary = "Backfill spell mechanics (056–062) into SPELL-owned rules (S2; dry-run unless apply=true)")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellRuleBackfillResult>>> backfillSpells(
            @RequestParam(defaultValue = "false") boolean apply) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(spellRuleBackfillService.backfill(apply),
                                apply ? "Бэкфилл заклинаний применён" : "Пробный прогон")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/batch-approve")
    @Operation(summary = "Batch-approve valid needs_review rules of a low-risk type")
    public CompletableFuture<ResponseEntity<ApiResponse<Integer>>> batchApprove(
            @RequestParam String ruleType, Authentication authentication) {
        final String username = usernameOf(authentication);
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                featureRuleAdminService.batchApproveLowRisk(ruleType, username), "Массовое утверждение")),
                controllerTaskExecutor);
    }

    @PostMapping("/feature-rules/maintenance/cleanup")
    @Operation(summary = "Cleanup: expire due effects, expire pending prompts, end stale transformations")
    public CompletableFuture<ResponseEntity<ApiResponse<FeatureMaintenanceResult>>> cleanup() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureRuntimeMaintenanceService.runCleanup(), "Очистка выполнена")),
                controllerTaskExecutor);
    }

    // ── Formulas (Stage 3) ──────────────────────────────────────────────────

    @GetMapping("/feature-formulas/vocabulary")
    @Operation(summary = "DSL vocabulary (functions + scalars) for the formula autocomplete; names from the evaluator allowlist")
    public CompletableFuture<ResponseEntity<ApiResponse<FormulaVocabularyResponse>>> formulaVocabulary(
            @RequestParam(name = "lang", required = false) String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(featureFormulaVocabularyService.vocabulary(lang))),
                controllerTaskExecutor);
    }

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
