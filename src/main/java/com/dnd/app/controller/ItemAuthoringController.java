package com.dnd.app.controller;

import com.dnd.app.dto.request.HomebrewItemRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.HomebrewItemResponse;
import com.dnd.app.service.homebrew.ItemAuthoringService;
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
 * Контроллер авторинга единого homebrew-предмета (P1.5 / IT-2). Пути package-scoped, по образцу классов/видов:
 * {@code /api/homebrew/packages/{packageId}/items[/{itemId}]}. Реализован kind=MAGIC.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Item Authoring", description = "Create/update homebrew items in a package")
public class ItemAuthoringController {

    private final ItemAuthoringService itemAuthoringService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/api/homebrew/packages/{packageId}/items/{itemId}")
    @Operation(summary = "Get a homebrew item for editing")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewItemResponse>>> get(
            @PathVariable UUID packageId, @PathVariable UUID itemId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(itemAuthoringService.get(packageId, itemId, auth.getName()))),
                controllerTaskExecutor);
    }

    @PostMapping("/api/homebrew/packages/{packageId}/items")
    @Operation(summary = "Create a homebrew item in a package (kind=MAGIC)")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewItemResponse>>> create(
            @PathVariable UUID packageId, @Valid @RequestBody HomebrewItemRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(itemAuthoringService.create(packageId, request, auth.getName()), "Предмет создан")),
                controllerTaskExecutor);
    }

    @PutMapping("/api/homebrew/packages/{packageId}/items/{itemId}")
    @Operation(summary = "Update a homebrew item")
    public CompletableFuture<ResponseEntity<ApiResponse<HomebrewItemResponse>>> update(
            @PathVariable UUID packageId, @PathVariable UUID itemId,
            @Valid @RequestBody HomebrewItemRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        itemAuthoringService.update(packageId, itemId, request, auth.getName()), "Предмет обновлён")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/api/homebrew/packages/{packageId}/items/{itemId}")
    @Operation(summary = "Delete a homebrew item")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(
            @PathVariable UUID packageId, @PathVariable UUID itemId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            itemAuthoringService.delete(packageId, itemId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Предмет удалён"));
        }, controllerTaskExecutor);
    }
}
