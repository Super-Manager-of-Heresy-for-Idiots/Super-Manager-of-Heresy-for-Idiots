package com.dnd.app.controller;

import com.dnd.app.domain.enums.DictionaryKind;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.DictionaryEntryResponse;
import com.dnd.app.dto.response.MonsterResponse;
import com.dnd.app.dto.response.MonsterSummaryResponse;
import com.dnd.app.service.BestiaryDictionaryService;
import com.dnd.app.service.MonsterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Authenticated read access to the bestiary: system reference dictionaries and
 * system monsters. Per-monster visibility (campaign scope) is enforced in the service.
 */
@RestController
@RequestMapping("/api/bestiary")
@RequiredArgsConstructor
@Tag(name = "Bestiary", description = "Browse system monsters and reference dictionaries")
public class BestiaryController {

    private final BestiaryDictionaryService dictionaryService;
    private final MonsterService monsterService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/dictionaries/{kind}")
    @Operation(summary = "List system dictionary entries")
    public CompletableFuture<ResponseEntity<ApiResponse<List<DictionaryEntryResponse>>>> listSystemDictionary(
            @PathVariable String kind) {
        return CompletableFuture.supplyAsync(() -> {
            List<DictionaryEntryResponse> data = dictionaryService.listSystem(DictionaryKind.fromSlug(kind));
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @GetMapping("/monsters")
    @Operation(summary = "List system monsters")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MonsterSummaryResponse>>>> listSystemMonsters(
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<MonsterSummaryResponse> data = monsterService.listSystemMonsters(auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @GetMapping("/monsters/{id}")
    @Operation(summary = "Get monster details")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> getMonster(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.getMonster(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }
}
