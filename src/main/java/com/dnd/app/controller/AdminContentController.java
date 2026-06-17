package com.dnd.app.controller;

import com.dnd.app.dto.content.ContentDataAuditReport;
import com.dnd.app.dto.content.ContentSeedSummary;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.ClassRewardSeedService;
import com.dnd.app.service.ContentDataAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Admin tools for the new content model: data-completeness reporting and a manual,
 * idempotent re-run of the content backfill. Secured under {@code /api/admin/**}.
 */
@RestController
@RequestMapping("/api/admin/content")
@RequiredArgsConstructor
@Tag(name = "Admin Content", description = "New content model data quality and backfill")
public class AdminContentController {

    private final ContentDataAuditService contentDataAuditService;
    private final ClassRewardSeedService classRewardSeedService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/audit")
    @Operation(summary = "Data-completeness report for the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentDataAuditReport>>> audit(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentDataAuditService.buildReport(lang))),
                controllerTaskExecutor);
    }

    @PostMapping("/backfill/subclass-choice-groups")
    @Operation(summary = "Idempotently backfill subclass-choice reward groups for core classes")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentSeedSummary>>> backfillSubclassChoiceGroups() {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(classRewardSeedService.seedCoreSubclassChoiceGroups())),
                controllerTaskExecutor);
    }
}
