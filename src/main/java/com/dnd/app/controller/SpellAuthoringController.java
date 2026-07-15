package com.dnd.app.controller;

import com.dnd.app.dto.request.HomebrewSpellRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.HomebrewSpellResponse;
import com.dnd.app.service.homebrew.SpellAuthoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Контроллер авторинга homebrew-заклинаний (P2-1). Пути — по образцу авторинга видов/предметов:
 * {@code /api/homebrew/packages/{packageId}/spells[/{spellId}]}. Механика заклинания исполняется движком
 * feature-rules (owner_type=SPELL) и добавляется отдельно.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Spell Authoring", description = "Create/update homebrew spells in a package")
public class SpellAuthoringController {

    private final SpellAuthoringService spellAuthoringService;
    private final Executor controllerTaskExecutor;

    /**
     * Читает заклинание пакета (для загрузки в редактор).
     */
    @GetMapping("/api/homebrew/packages/{packageId}/spells/{spellId}")
    @Operation(summary = "Get a homebrew spell for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewSpellResponse>>> get(
            @PathVariable UUID packageId, @PathVariable UUID spellId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(spellAuthoringService.get(packageId, spellId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Создаёт заклинание в пакете.
     */
    @PostMapping("/api/homebrew/packages/{packageId}/spells")
    @Operation(summary = "Create a homebrew spell in a package")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewSpellResponse>>> create(
            @PathVariable UUID packageId, @Valid @RequestBody HomebrewSpellRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(spellAuthoringService.create(packageId, request, auth.getName()),
                                "Заклинание создано")),
                controllerTaskExecutor);
    }

    /**
     * Обновляет заклинание пакета.
     */
    @PutMapping("/api/homebrew/packages/{packageId}/spells/{spellId}")
    @Operation(summary = "Update a homebrew spell")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewSpellResponse>>> update(
            @PathVariable UUID packageId, @PathVariable UUID spellId,
            @Valid @RequestBody HomebrewSpellRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        spellAuthoringService.update(packageId, spellId, request, auth.getName()),
                        "Заклинание обновлено")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет заклинание пакета (409 при использовании).
     */
    @DeleteMapping("/api/homebrew/packages/{packageId}/spells/{spellId}")
    @Operation(summary = "Delete a homebrew spell")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable UUID packageId, @PathVariable UUID spellId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            spellAuthoringService.delete(packageId, spellId, auth.getName());
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Заклинание удалено"));
        }, controllerTaskExecutor);
    }
}
