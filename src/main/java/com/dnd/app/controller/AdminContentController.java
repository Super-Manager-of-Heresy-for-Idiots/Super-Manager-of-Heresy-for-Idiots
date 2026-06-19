package com.dnd.app.controller;

import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.ContentDataAuditReport;
import com.dnd.app.dto.content.ContentDataQualityReport;
import com.dnd.app.dto.content.ContentSeedSummary;
import com.dnd.app.dto.content.ImportWarningResponse;
import com.dnd.app.dto.content.RuntimeMigrationReport;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.ClassRewardSeedService;
import com.dnd.app.service.ContentDataAuditService;
import com.dnd.app.service.ContentReferenceService;
import com.dnd.app.service.RuntimeDataMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Admin tools for the new content model: read views of the same model the runtime
 * uses, data-completeness &amp; data-quality reports, the import-warning viewer, and an
 * idempotent backfill. Secured under {@code /api/admin/**} (ADMIN role).
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
    private final Executor controllerTaskExecutor;

    // --- read views (same model runtime uses) ---

    @GetMapping("/classes")
    @Operation(summary = "List core content classes (admin)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentClassDetailResponse>>>> listClasses(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaClasses(lang))),
                controllerTaskExecutor);
    }

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

    @GetMapping("/audit")
    @Operation(summary = "Data-completeness report for the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentDataAuditReport>>> audit(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentDataAuditService.buildReport(lang))),
                controllerTaskExecutor);
    }

    @GetMapping("/data-quality")
    @Operation(summary = "Data-quality findings (features without rewards, grants without payload, orphans)")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentDataQualityReport>>> dataQuality() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentDataAuditService.buildDataQualityReport())),
                controllerTaskExecutor);
    }

    @GetMapping("/import-warnings")
    @Operation(summary = "View recorded content import warnings")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ImportWarningResponse>>>> importWarnings() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentDataAuditService.listImportWarnings())),
                controllerTaskExecutor);
    }

    // --- runtime data migration (Phase 10) ---

    @PostMapping("/runtime-migration")
    @Operation(summary = "Migrate legacy runtime IDs (class_id/skill_id) to new content IDs; "
            + "dry-run by default, applying requires confirmBackup=true")
    public CompletableFuture<ResponseEntity<ApiResponse<RuntimeMigrationReport>>> runtimeMigration(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(defaultValue = "false") boolean confirmBackup) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                runtimeDataMigrationService.migrate(dryRun, confirmBackup))),
                controllerTaskExecutor);
    }

    // --- backfill ---

    @PostMapping("/backfill/subclass-choice-groups")
    @Operation(summary = "Idempotently backfill subclass-choice reward groups for core classes")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentSeedSummary>>> backfillSubclassChoiceGroups() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(classRewardSeedService.seedCoreSubclassChoiceGroups())),
                controllerTaskExecutor);
    }
}
