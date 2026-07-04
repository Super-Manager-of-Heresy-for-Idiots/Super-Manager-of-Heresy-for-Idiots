package com.dnd.app.controller;

import com.dnd.app.dto.content.CharacterClassFeatureResponse;
import com.dnd.app.dto.featurerule.CapabilityProfileResponse;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.security.CharacterAccessGuard;
import com.dnd.app.service.CharacterCapabilityProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Class-aware capability profile for a character — the single read the frontend uses to decide which
 * panels/tabs to render (see {@code docs/FEATURE_RULES_FRONTEND_REWORK_PLAN.md} §0). The spellcasting block
 * is always populated from class content; feature-rules presence flags reflect the active
 * {@code app.feature-rules.*} subsystems. Access = owner / campaign GM / ADMIN.
 */
@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
@Tag(name = "Character Capability Profile", description = "Class-aware UI capability profile")
public class CharacterCapabilityController {

    private final CharacterCapabilityProfileService capabilityProfileService;
    private final CharacterAccessGuard accessGuard;
    private final Executor controllerTaskExecutor;

    @GetMapping("/{characterId}/capability-profile")
    @Operation(summary = "Get a character's class-aware capability profile")
    public CompletableFuture<ResponseEntity<ApiResponse<CapabilityProfileResponse>>> capabilityProfile(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = authentication != null ? authentication.getName() : null;
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(capabilityProfileService.build(characterId)));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{characterId}/class-features")
    @Operation(summary = "Get a character's structured class features (the real abilities, for the Features tab)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CharacterClassFeatureResponse>>>> classFeatures(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = authentication != null ? authentication.getName() : null;
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(capabilityProfileService.listClassFeatures(characterId)));
        }, controllerTaskExecutor);
    }
}
