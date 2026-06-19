package com.dnd.app.controller;

import com.dnd.app.dto.content.LevelUpOptionsResponse;
import com.dnd.app.dto.content.LevelUpRequest;
import com.dnd.app.dto.content.LevelUpResultResponse;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.LevelUpCommandService;
import com.dnd.app.service.LevelUpQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Level-up endpoints on the new content model. The in-place routes are the final
 * contract; the /content aliases are kept during rollout.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Content Level-Up", description = "New content-model level-up read & commit flow")
public class ContentLevelUpController {

    private final LevelUpQueryService levelUpQueryService;
    private final LevelUpCommandService levelUpCommandService;
    private final Executor controllerTaskExecutor;

    @GetMapping({
            "/api/characters/{characterId}/level-up-options",
            "/api/characters/{characterId}/content/level-up-options"
    })
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

    @PostMapping({
            "/api/characters/{characterId}/level-up",
            "/api/characters/{characterId}/content/level-up"
    })
    @Operation(summary = "Commit a level-up, persisting reward selections to the new content model")
    public CompletableFuture<ResponseEntity<ApiResponse<LevelUpResultResponse>>> commitLevelUp(
            @PathVariable UUID characterId,
            @RequestParam(defaultValue = "en") String lang,
            @Valid @RequestBody LevelUpRequest request,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                        ResponseEntity.ok(ApiResponse.ok(
                                levelUpCommandService.commitLevelUp(characterId, auth.getName(), request, lang),
                                "Уровень повышен")),
                controllerTaskExecutor);
    }
}
