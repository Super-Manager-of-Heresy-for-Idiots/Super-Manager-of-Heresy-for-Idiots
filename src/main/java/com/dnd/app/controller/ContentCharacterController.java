package com.dnd.app.controller;

import com.dnd.app.dto.content.ContentCharacterCreationResponse;
import com.dnd.app.dto.request.CreateContentCharacterRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.ContentCharacterCreationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Character creation on the new content model (Phase 5). Parallel to the legacy
 * wizard endpoints, which remain available during the migration.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Content Character Creation", description = "Create characters from the new content model")
public class ContentCharacterController {

    private final ContentCharacterCreationService creationService;
    private final Executor controllerTaskExecutor;

    @PostMapping("/api/campaigns/{campaignId}/characters/content")
    @Operation(summary = "Create a campaign character from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentCharacterCreationResponse>>> createCampaignCharacter(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CreateContentCharacterRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ContentCharacterCreationResponse result =
                    creationService.createCampaignCharacter(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Персонаж создан"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/api/characters/content")
    @Operation(summary = "Create a vanilla character template from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<ContentCharacterCreationResponse>>> createVanillaCharacter(
            @Valid @RequestBody CreateContentCharacterRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ContentCharacterCreationResponse result =
                    creationService.createVanillaCharacter(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result, "Шаблон персонажа создан"));
        }, controllerTaskExecutor);
    }
}
