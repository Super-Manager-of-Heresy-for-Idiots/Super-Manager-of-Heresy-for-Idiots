package com.dnd.app.controller;

import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.CharacterKnownSpellResponse;
import com.dnd.app.security.CharacterAccessGuard;
import com.dnd.app.service.CharacterSpellbookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
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
 * Spellbook management for a character: record newly learned spells and forget them, outside the
 * level-up flow (the folio was missing this — a Wizard could not record spells). Access = owner/GM/ADMIN.
 */
@RestController
@RequestMapping("/api/characters/{characterId}/spellbook")
@RequiredArgsConstructor
@Tag(name = "Character Spellbook", description = "Record/forget a character's known spells")
public class CharacterSpellbookController {

    private final CharacterSpellbookService spellbookService;
    private final CharacterAccessGuard accessGuard;
    private final Executor controllerTaskExecutor;

    @GetMapping
    @Operation(summary = "List a character's recorded spells")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CharacterKnownSpellResponse>>>> list(
            @PathVariable UUID characterId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(spellbookService.list(characterId)));
        }, controllerTaskExecutor);
    }

    @PostMapping
    @Operation(summary = "Record (learn) a spell into the character's spellbook")
    public CompletableFuture<ResponseEntity<ApiResponse<CharacterKnownSpellResponse>>> learn(
            @PathVariable UUID characterId, @RequestParam UUID spellId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            return ResponseEntity.ok(ApiResponse.ok(spellbookService.learn(characterId, spellId), "Заклинание записано"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{spellId}")
    @Operation(summary = "Forget (remove) a spell from the character's spellbook")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> forget(
            @PathVariable UUID characterId, @PathVariable UUID spellId, Authentication authentication) {
        final String username = username(authentication);
        return CompletableFuture.supplyAsync(() -> {
            accessGuard.require(characterId, username);
            spellbookService.forget(characterId, spellId);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Заклинание удалено"));
        }, controllerTaskExecutor);
    }

    private static String username(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
