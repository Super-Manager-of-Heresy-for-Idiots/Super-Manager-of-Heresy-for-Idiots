package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateItemTemplateRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.ItemTemplateResponse;
import com.dnd.app.service.ItemTemplateService;
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

@RestController
@RequestMapping("/api/item-templates")
@RequiredArgsConstructor
@Tag(name = "Item Templates", description = "Item template management")
public class ItemTemplateController {

    private final ItemTemplateService itemTemplateService;
    private final Executor controllerTaskExecutor;

    @PostMapping
    @Operation(summary = "Create item template (Admin/GM)")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTemplateResponse>>> createTemplate(
            @Valid @RequestBody CreateItemTemplateRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ItemTemplateResponse response = itemTemplateService.createTemplate(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response, "Item template created"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get item template")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTemplateResponse>>> getTemplate(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            ItemTemplateResponse response = itemTemplateService.getTemplate(id);
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    @GetMapping("/campaign/{campaignId}")
    @Operation(summary = "List available item templates for campaign")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ItemTemplateResponse>>>> listTemplates(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<ItemTemplateResponse> templates = itemTemplateService.listTemplates(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(templates));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update item template")
    public CompletableFuture<ResponseEntity<ApiResponse<ItemTemplateResponse>>> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody CreateItemTemplateRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ItemTemplateResponse response = itemTemplateService.updateTemplate(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Item template updated"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete item template (Admin only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteTemplate(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            itemTemplateService.deleteTemplate(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Item template deleted"));
        }, controllerTaskExecutor);
    }
}
