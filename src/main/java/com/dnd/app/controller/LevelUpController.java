package com.dnd.app.controller;

import com.dnd.app.dto.request.LevelUpRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.CharacterRewardQueryService;
import com.dnd.app.service.LevelUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class LevelUpController {

    private final LevelUpService levelUpService;
    private final CharacterRewardQueryService rewardQueryService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/{id}/level-up-options")
    public CompletableFuture<ResponseEntity<ApiResponse<LevelUpOptionsResponse>>> getLevelUpOptions(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LevelUpOptionsResponse options = levelUpService.getLevelUpOptions(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(options));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{id}/level-up")
    public CompletableFuture<ResponseEntity<ApiResponse<LevelUpResultResponse>>> levelUp(
            @PathVariable UUID id, @Valid @RequestBody LevelUpRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            LevelUpResultResponse result = levelUpService.commitLevelUp(id, auth.getName(), request);
            return ResponseEntity.ok(ApiResponse.ok(result, "Уровень повышен"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}/rewards")
    public CompletableFuture<ResponseEntity<ApiResponse<CharacterRewardsResponse>>> getRewards(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CharacterRewardsResponse rewards = rewardQueryService.getCharacterRewards(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(rewards));
        }, controllerTaskExecutor);
    }
}
