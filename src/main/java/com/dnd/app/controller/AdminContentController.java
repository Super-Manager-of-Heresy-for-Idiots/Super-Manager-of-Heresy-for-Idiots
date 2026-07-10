package com.dnd.app.controller;

import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.ContentDataAuditReport;
import com.dnd.app.dto.content.ContentDataQualityReport;
import com.dnd.app.dto.content.ContentSeedSummary;
import com.dnd.app.dto.content.ClassFeatureWarningResponse;
import com.dnd.app.dto.content.ImportWarningResponse;
import com.dnd.app.dto.content.RuntimeMigrationReport;
import com.dnd.app.dto.content.SpellDetailResponse;
import com.dnd.app.dto.content.SpellWarningResponse;
import com.dnd.app.dto.request.ClassFeatureResolutionRequest;
import com.dnd.app.dto.request.SetSpellBuffsRequest;
import com.dnd.app.dto.request.SpellEditRequest;
import com.dnd.app.dto.request.SpellResolutionRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BuffDebuffResponse;
import com.dnd.app.service.ClassFeatureAdminService;
import com.dnd.app.service.ClassRewardSeedService;
import com.dnd.app.service.ContentDataAuditService;
import com.dnd.app.service.ContentReferenceService;
import com.dnd.app.service.RuntimeDataMigrationService;
import com.dnd.app.service.SpellAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Класс AdminContentController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
@Tag(name = "Admin Content", description = "New content model admin views, data quality and backfill")
public class AdminContentController {

    private final ContentDataAuditService contentDataAuditService;
    private final ContentReferenceService contentReferenceService;
    private final ClassRewardSeedService classRewardSeedService;
    private final RuntimeDataMigrationService runtimeDataMigrationService;
    private final SpellAdminService spellAdminService;
    private final ClassFeatureAdminService classFeatureAdminService;
    private final Executor controllerTaskExecutor;

    // --- read views (same model runtime uses) ---

    /**
     * Возвращает список для операции "list classes" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/classes")
    @Operation(summary = "List core content classes (admin)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentClassDetailResponse>>>> listClasses(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaClasses(lang))),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get class" в рамках бизнес-логики API.
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/classes/{classId}")
    @Operation(summary = "Get a core content class with its full graph (admin)")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentClassDetailResponse>>> getClass(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaClass(classId, lang))),
                controllerTaskExecutor);
    }

    // --- data quality / completeness ---

    /**
     * Выполняет операции "audit" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/audit")
    @Operation(summary = "Data-completeness report for the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentDataAuditReport>>> audit(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentDataAuditService.buildReport(lang))),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "data quality" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/data-quality")
    @Operation(summary = "Data-quality findings (features without rewards, grants without payload, orphans)")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentDataQualityReport>>> dataQuality() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentDataAuditService.buildDataQualityReport())),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "import warnings" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/import-warnings")
    @Operation(summary = "View recorded content import warnings")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ImportWarningResponse>>>> importWarnings() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentDataAuditService.listImportWarnings())),
                controllerTaskExecutor);
    }

    // --- spell resolution review (data-quality) ---

    /**
     * Выполняет операции "spell warnings" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/spell-warnings")
    @Operation(summary = "List spells flagged for manual resolution review (unparsed save ability, etc.)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellWarningResponse>>>> spellWarnings(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(spellAdminService.listWarnings(lang))),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "class feature warnings" в рамках бизнес-логики API.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/class-feature-warnings")
    @Operation(summary = "List class features flagged for manual mechanics review")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ClassFeatureWarningResponse>>>> classFeatureWarnings(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(classFeatureAdminService.listWarnings(lang))),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "resolve spell" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PatchMapping("/spells/{id}/resolution")
    @Operation(summary = "Apply an admin correction of a spell's save ability / attack roll and clear its warning")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellWarningResponse>>> resolveSpell(
            @PathVariable UUID id,
            @RequestBody SpellResolutionRequest request,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(spellAdminService.resolve(id, request, lang))),
                controllerTaskExecutor);
    }

    /**
     * Выполняет операции "resolve class feature" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PatchMapping("/class-features/{id}/resolution")
    @Operation(summary = "Apply an admin correction of a class feature's parsed mechanics and clear its warning")
    public CompletableFuture<ResponseEntity<ApiResponse<ClassFeatureWarningResponse>>> resolveClassFeature(
            @PathVariable UUID id,
            @RequestBody ClassFeatureResolutionRequest request,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(classFeatureAdminService.resolve(id, request, lang))),
                controllerTaskExecutor);
    }

    /**
     * Обновляет результат операции "update spell" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/spells/{id}")
    @Operation(summary = "Full admin edit of a spell's resolution: damage, healing, save/attack, ability check, warning")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellDetailResponse>>> updateSpell(
            @PathVariable UUID id,
            @RequestBody SpellEditRequest request,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(spellAdminService.update(id, request, lang), "Заклинание обновлено")),
                controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get spell buffs" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @GetMapping("/spells/{id}/buffs")
    @Operation(summary = "List the buffs/debuffs linked to a spell")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BuffDebuffResponse>>>> getSpellBuffs(
            @PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(spellAdminService.getLinkedBuffs(id))),
                controllerTaskExecutor);
    }

    /**
     * Устанавливает результат операции "set spell buffs" в рамках бизнес-логики API.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @return результат выполнения бизнес-операции
     */
    @PutMapping("/spells/{id}/buffs")
    @Operation(summary = "Replace the set of buffs/debuffs a spell applies")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BuffDebuffResponse>>>> setSpellBuffs(
            @PathVariable UUID id,
            @RequestBody SetSpellBuffsRequest request) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                spellAdminService.setLinkedBuffs(id, request.getBuffDebuffIds()),
                                "Эффекты заклинания обновлены")),
                controllerTaskExecutor);
    }

    // --- runtime data migration (Phase 10) ---

    /**
     * Выполняет операции "runtime migration" в рамках бизнес-логики API.
     * @param dryRun входящее значение dry run, используемое бизнес-сценарием
     * @param confirmBackup входящее значение confirm backup, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/runtime-migration")
    @Operation(summary = "Migrate legacy runtime IDs (class_id/skill_id) to new content IDs; "
            + "dry-run by default, applying requires confirmBackup=true")
    /**
     * Выполняет операции "runtime migration" в рамках бизнес-логики API.
     * @param dryRun входящее значение dry run, используемое бизнес-сценарием
     * @param confirmBackup входящее значение confirm backup, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public CompletableFuture<ResponseEntity<ApiResponse<RuntimeMigrationReport>>> runtimeMigration(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean confirmBackup) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                runtimeDataMigrationService.migrate(dryRun, confirmBackup))),
                controllerTaskExecutor);
    }

    // --- backfill ---

    /**
     * Выполняет обратное заполнение операции "backfill subclass choice groups" в рамках бизнес-логики API.
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/backfill/subclass-choice-groups")
    @Operation(summary = "Idempotently backfill subclass-choice reward groups for core classes")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentSeedSummary>>> backfillSubclassChoiceGroups() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(classRewardSeedService.seedCoreSubclassChoiceGroups())),
                controllerTaskExecutor);
    }
}
