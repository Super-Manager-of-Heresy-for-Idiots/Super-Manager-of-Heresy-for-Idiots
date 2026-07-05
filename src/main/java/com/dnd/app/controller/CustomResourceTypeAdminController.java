package com.dnd.app.controller;

import com.dnd.app.dto.request.CustomResourceTypeRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.CustomResourceTypeAdminResponse;
import com.dnd.app.service.CustomResourceTypeAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Admin editor for class resource templates ({@code custom_resource_types}) — the single, player-facing
 * resource system. URL-gated to ADMIN by {@code /api/admin/**}.
 */
@RestController
@RequestMapping("/api/admin/resource-types")
@RequiredArgsConstructor
@Tag(name = "Custom Resource Types (admin)", description = "Class resource templates: max, formula, class binding")
public class CustomResourceTypeAdminController {

    private final CustomResourceTypeAdminService service;
    private final Executor controllerTaskExecutor;

    @GetMapping
    @Operation(summary = "List vanilla class resource templates")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CustomResourceTypeAdminResponse>>>> list() {
        return CompletableFuture.supplyAsync(() -> ResponseEntity.ok(ApiResponse.ok(service.list())),
                controllerTaskExecutor);
    }

    @PostMapping
    @Operation(summary = "Create a class resource template")
    public CompletableFuture<ResponseEntity<ApiResponse<CustomResourceTypeAdminResponse>>> create(
            @Valid @RequestBody CustomResourceTypeRequest request) {
        return CompletableFuture.supplyAsync(
                () -> ResponseEntity.ok(ApiResponse.ok(service.create(request), "Ресурс создан")),
                controllerTaskExecutor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a class resource template (max / formula / class binding)")
    public CompletableFuture<ResponseEntity<ApiResponse<CustomResourceTypeAdminResponse>>> update(
            @PathVariable UUID id, @Valid @RequestBody CustomResourceTypeRequest request) {
        return CompletableFuture.supplyAsync(
                () -> ResponseEntity.ok(ApiResponse.ok(service.update(id, request), "Ресурс сохранён")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a class resource template (also removes its per-character instances)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            service.delete(id);
            return ResponseEntity.ok(ApiResponse.<Void>ok(null, "Ресурс удалён"));
        }, controllerTaskExecutor);
    }
}

