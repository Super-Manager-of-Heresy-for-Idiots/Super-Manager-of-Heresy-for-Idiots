package com.dnd.app.controller;

import com.dnd.app.dto.content.LevelUpOptionsResponse;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.LevelUpQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Level-up READ endpoints on the new content model (Phase 6). Parallel to the legacy
 * level-up endpoints, which remain available until commit is migrated (Phase 7) and
 * the legacy routes are removed (Phases 11/12).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Content Level-Up", description = "New content-model level-up read flow")
public class ContentLevelUpController {

    private final LevelUpQueryService levelUpQueryService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/api/characters/{characterId}/content/level-up-options")
    @Operation(summary = "Get level-up options (reward groups/options/grants) from the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<LevelUpOptionsResponse>>> getLevelUpOptions(
            @PathVariable UUID characterId,
            @RequestParam(defaultValue = "en") String lang,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                levelUpQueryService.getLevelUpOptions(characterId, auth.getName(), lang))),
                controllerTaskExecutor);
    }
}
