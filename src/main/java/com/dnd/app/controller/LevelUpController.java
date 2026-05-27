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

@RestController
@RequestMapping("/api/characters")
@RequiredArgsConstructor
public class LevelUpController {

    private final LevelUpService levelUpService;
    private final CharacterRewardQueryService rewardQueryService;

    @GetMapping("/{id}/level-up-options")
    public ResponseEntity<ApiResponse<LevelUpOptionsResponse>> getLevelUpOptions(
            @PathVariable UUID id, Authentication auth) {
        LevelUpOptionsResponse options = levelUpService.getLevelUpOptions(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(options));
    }

    @PostMapping("/{id}/level-up")
    public ResponseEntity<ApiResponse<LevelUpResultResponse>> levelUp(
            @PathVariable UUID id, @Valid @RequestBody LevelUpRequest request, Authentication auth) {
        LevelUpResultResponse result = levelUpService.commitLevelUp(id, auth.getName(), request);
        return ResponseEntity.ok(ApiResponse.ok(result, "Level up successful"));
    }

    @GetMapping("/{id}/rewards")
    public ResponseEntity<ApiResponse<CharacterRewardsResponse>> getRewards(
            @PathVariable UUID id, Authentication auth) {
        CharacterRewardsResponse rewards = rewardQueryService.getCharacterRewards(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(rewards));
    }
}
