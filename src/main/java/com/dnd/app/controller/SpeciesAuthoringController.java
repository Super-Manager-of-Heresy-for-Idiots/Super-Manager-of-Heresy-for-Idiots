package com.dnd.app.controller;

import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.service.homebrew.SpeciesAuthoringService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Контроллер авторинга homebrew-видов (P0.5 / SP-1, SP-2). Пути — по образцу авторинга классов:
 * {@code /api/homebrew/packages/{packageId}/species[/{speciesId}]}. Старые пути
 * {@code /homebrew/my/{id}/content/races} не возрождаются (легаси, удалены в S5).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Species Authoring", description = "Create/update homebrew species in a package")
public class SpeciesAuthoringController {

    private final SpeciesAuthoringService speciesAuthoringService;
    private final Executor controllerTaskExecutor;

    /**
     * Читает вид пакета (для загрузки в редактор).
     */
    @GetMapping("/api/homebrew/packages/{packageId}/species/{speciesId}")
    @Operation(summary = "Get a homebrew species for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<JsonNode>>> get(
            @PathVariable UUID packageId, @PathVariable UUID speciesId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(speciesAuthoringService.get(packageId, speciesId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Создаёт вид в пакете.
     */
    @PostMapping("/api/homebrew/packages/{packageId}/species")
    @Operation(summary = "Create a homebrew species in a package")
    public CompletableFuture<ResponseEntity<ApiResponse<JsonNode>>> create(
            @PathVariable UUID packageId, @RequestBody JsonNode body, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(speciesAuthoringService.create(packageId, body, auth.getName()), "Вид создан")),
                controllerTaskExecutor);
    }

    /**
     * Обновляет вид пакета.
     */
    @PutMapping("/api/homebrew/packages/{packageId}/species/{speciesId}")
    @Operation(summary = "Update a homebrew species")
    public CompletableFuture<ResponseEntity<ApiResponse<JsonNode>>> update(
            @PathVariable UUID packageId, @PathVariable UUID speciesId,
            @RequestBody JsonNode body, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        speciesAuthoringService.update(packageId, speciesId, body, auth.getName()), "Вид обновлён")),
                controllerTaskExecutor);
    }

    /**
     * Удаляет вид пакета (409 при зависимых персонажах).
     */
    @DeleteMapping("/api/homebrew/packages/{packageId}/species/{speciesId}")
    @Operation(summary = "Delete a homebrew species")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable UUID packageId, @PathVariable UUID speciesId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            speciesAuthoringService.delete(packageId, speciesId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Вид удалён"));
        }, controllerTaskExecutor);
    }

    /**
     * Включает вид.
     */
    @PostMapping("/api/homebrew/packages/{packageId}/species/{speciesId}/enable")
    @Operation(summary = "Enable a homebrew species")
    public CompletableFuture<ResponseEntity<ApiResponse<JsonNode>>> enable(
            @PathVariable UUID packageId, @PathVariable UUID speciesId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(speciesAuthoringService.enable(packageId, speciesId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * Выключает вид.
     */
    @PostMapping("/api/homebrew/packages/{packageId}/species/{speciesId}/disable")
    @Operation(summary = "Disable a homebrew species")
    public CompletableFuture<ResponseEntity<ApiResponse<JsonNode>>> disable(
            @PathVariable UUID packageId, @PathVariable UUID speciesId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(speciesAuthoringService.disable(packageId, speciesId, auth.getName()))),
                controllerTaskExecutor);
    }

    /**
     * SP-2: глубокий клон vanilla-вида в пакет.
     */
    @PostMapping("/api/homebrew/packages/{packageId}/species/duplicate-from/{vanillaSpeciesId}")
    @Operation(summary = "Duplicate a vanilla species into a homebrew package")
    public CompletableFuture<ResponseEntity<ApiResponse<JsonNode>>> duplicate(
            @PathVariable UUID packageId, @PathVariable UUID vanillaSpeciesId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                        speciesAuthoringService.duplicateFromVanilla(packageId, vanillaSpeciesId, auth.getName()),
                        "Вид скопирован")),
                controllerTaskExecutor);
    }
}
