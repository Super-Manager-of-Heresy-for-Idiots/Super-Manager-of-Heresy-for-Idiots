package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateEnchantmentTypeRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.EnchantmentTypeResponse;
import com.dnd.app.service.EnchantmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequiredArgsConstructor
public class EnchantmentTypeController {

    private final EnchantmentService enchantmentService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/api/admin/enchantment-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EnchantmentTypeResponse>>>> listAdmin() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.listEnchantmentTypes())),
                controllerTaskExecutor);
    }

    @PostMapping("/api/admin/enchantment-types")
    public CompletableFuture<ResponseEntity<ApiResponse<EnchantmentTypeResponse>>> create(
            @Valid @RequestBody CreateEnchantmentTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(enchantmentService.createEnchantmentType(request), "Тип зачарования создан")),
                controllerTaskExecutor);
    }

    @GetMapping("/api/admin/enchantment-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<EnchantmentTypeResponse>>> get(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.getEnchantmentType(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/api/admin/enchantment-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<EnchantmentTypeResponse>>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateEnchantmentTypeRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.updateEnchantmentType(id, request), "Тип зачарования обновлен")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/api/admin/enchantment-types/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            enchantmentService.deleteEnchantmentType(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Тип зачарования удален"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/api/enchantment-types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<EnchantmentTypeResponse>>>> listPublic() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(enchantmentService.listEnchantmentTypes())),
                controllerTaskExecutor);
    }
}
