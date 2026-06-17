package com.dnd.app.controller;

import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.ContentReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Read-only reference endpoints for the new normalized content model. The in-place
 * routes are the final contract; the /content aliases are kept during rollout.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Content Reference", description = "New normalized content model (classes, features, reward groups)")
public class ContentReferenceController {

    private final ContentReferenceService contentReferenceService;
    private final Executor controllerTaskExecutor;

    @GetMapping({"/api/reference/classes", "/api/reference/content/classes"})
    @Operation(summary = "Get core (vanilla) classes from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentClassDetailResponse>>>> getVanillaClasses(
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaClasses(lang))),
                controllerTaskExecutor);
    }

    @GetMapping({"/api/reference/classes/{classId}", "/api/reference/content/classes/{classId}"})
    @Operation(summary = "Get a single core (vanilla) class from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentClassDetailResponse>>> getVanillaClass(
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(contentReferenceService.getVanillaClass(classId, lang))),
                controllerTaskExecutor);
    }

    @GetMapping({
            "/api/campaigns/{campaignId}/reference/classes",
            "/api/campaigns/{campaignId}/reference/content/classes"
    })
    @Operation(summary = "Get campaign-visible classes (core + active homebrew) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ContentClassDetailResponse>>>> getCampaignClasses(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentReferenceService.getCampaignClasses(campaignId, auth.getName(), lang))),
                controllerTaskExecutor);
    }

    @GetMapping({
            "/api/campaigns/{campaignId}/reference/classes/{classId}",
            "/api/campaigns/{campaignId}/reference/content/classes/{classId}"
    })
    @Operation(summary = "Get a single campaign-visible class from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentClassDetailResponse>>> getCampaignClass(
            @PathVariable UUID campaignId,
            @PathVariable UUID classId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                contentReferenceService.getCampaignClass(campaignId, classId, auth.getName(), lang))),
                controllerTaskExecutor);
    }
}
