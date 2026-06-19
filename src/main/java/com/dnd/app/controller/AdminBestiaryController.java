package com.dnd.app.controller;

import com.dnd.app.domain.enums.DictionaryKind;
import com.dnd.app.dto.request.DictionaryEntryRequest;
import com.dnd.app.dto.request.MonsterRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.DictionaryEntryResponse;
import com.dnd.app.dto.response.MonsterResponse;
import com.dnd.app.dto.response.MonsterSummaryResponse;
import com.dnd.app.service.BestiaryDictionaryService;
import com.dnd.app.service.MonsterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * ADMIN-only bestiary management (URL-gated by /api/admin/** in SecurityConfig):
 * system reference dictionaries + system monsters.
 */
@RestController
@RequestMapping("/api/admin/bestiary")
@RequiredArgsConstructor
@Tag(name = "Admin Bestiary", description = "System monsters and reference dictionaries (ADMIN)")
public class AdminBestiaryController {

    private final BestiaryDictionaryService dictionaryService;
    private final MonsterService monsterService;
    private final Executor controllerTaskExecutor;

    // === System dictionaries ===

    @GetMapping("/dictionaries/{kind}")
    @Operation(summary = "List system dictionary entries")
    public CompletableFuture<ResponseEntity<ApiResponse<List<DictionaryEntryResponse>>>> listSystem(
            @PathVariable String kind) {
        return CompletableFuture.supplyAsync(() -> {
            List<DictionaryEntryResponse> data = dictionaryService.listSystem(DictionaryKind.fromSlug(kind));
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @PostMapping("/dictionaries/{kind}")
    @Operation(summary = "Create system dictionary entry")
    public CompletableFuture<ResponseEntity<ApiResponse<DictionaryEntryResponse>>> createSystem(
            @PathVariable String kind,
            @Valid @RequestBody DictionaryEntryRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            DictionaryEntryResponse data = dictionaryService.createSystem(DictionaryKind.fromSlug(kind), request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Dictionary entry created"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/dictionaries/{kind}/{id}")
    @Operation(summary = "Update system dictionary entry")
    public CompletableFuture<ResponseEntity<ApiResponse<DictionaryEntryResponse>>> updateSystem(
            @PathVariable String kind,
            @PathVariable UUID id,
            @Valid @RequestBody DictionaryEntryRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            DictionaryEntryResponse data = dictionaryService.updateSystem(DictionaryKind.fromSlug(kind), id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Dictionary entry updated"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/dictionaries/{kind}/{id}")
    @Operation(summary = "Delete system dictionary entry")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteSystem(
            @PathVariable String kind,
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            dictionaryService.deleteSystem(DictionaryKind.fromSlug(kind), id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Dictionary entry deleted"));
        }, controllerTaskExecutor);
    }

    // === System monsters ===

    @GetMapping("/monsters")
    @Operation(summary = "List system monsters")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MonsterSummaryResponse>>>> listMonsters(Authentication auth) {
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

    @PostMapping("/monsters")
    @Operation(summary = "Create system monster")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> createMonster(
            @Valid @RequestBody MonsterRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.createSystemMonster(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Monster created"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/monsters/{id}")
    @Operation(summary = "Update system monster")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> updateMonster(
            @PathVariable UUID id,
            @Valid @RequestBody MonsterRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.updateSystemMonster(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Monster updated"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/monsters/{id}/active")
    @Operation(summary = "Activate or deactivate a system monster")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> setActive(
            @PathVariable UUID id,
            @RequestParam boolean active, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.setSystemMonsterActive(id, active, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Monster active flag updated"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/monsters/{id}")
    @Operation(summary = "Delete system monster")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteMonster(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            monsterService.deleteSystemMonster(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Monster deleted"));
        }, controllerTaskExecutor);
    }
}
