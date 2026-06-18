package com.dnd.app.controller;

import com.dnd.app.dto.response.*;
import com.dnd.app.service.CharacterRewardQueryService;
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

    private final CharacterRewardQueryService rewardQueryService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/{id}/rewards")
    public CompletableFuture<ResponseEntity<ApiResponse<CharacterRewardsResponse>>> getRewards(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CharacterRewardsResponse rewards = rewardQueryService.getCharacterRewards(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(rewards));
        }, controllerTaskExecutor);
    }
}
