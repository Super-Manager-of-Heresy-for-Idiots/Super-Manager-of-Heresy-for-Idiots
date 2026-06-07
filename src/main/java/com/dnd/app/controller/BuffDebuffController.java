package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateBuffDebuffRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BuffDebuffResponse;
import com.dnd.app.service.BuffDebuffService;
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
@RequestMapping("/api/admin/buffs-debuffs")
@RequiredArgsConstructor
public class BuffDebuffController {

    private final BuffDebuffService buffDebuffService;
    private final Executor controllerTaskExecutor;

    @GetMapping
    public CompletableFuture<ResponseEntity<ApiResponse<List<BuffDebuffResponse>>>> list(
            @RequestParam(required = false) Boolean isBuff,
            @RequestParam(required = false) String effectType) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(buffDebuffService.findAll(isBuff, effectType))),
                controllerTaskExecutor);
    }

    @PostMapping
    public CompletableFuture<ResponseEntity<ApiResponse<BuffDebuffResponse>>> create(
            @Valid @RequestBody CreateBuffDebuffRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.ok(buffDebuffService.create(request), "Бафф/дебафф создан")),
                controllerTaskExecutor);
    }

    @GetMapping("/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BuffDebuffResponse>>> get(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(buffDebuffService.findById(id))),
                controllerTaskExecutor);
    }

    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<BuffDebuffResponse>>> update(
            @PathVariable UUID id, @Valid @RequestBody CreateBuffDebuffRequest request) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(buffDebuffService.update(id, request), "Бафф/дебафф обновлен")),
                controllerTaskExecutor);
    }

    @DeleteMapping("/{id}")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> delete(@PathVariable UUID id) {
        return CompletableFuture.supplyAsync(() -> {
            buffDebuffService.delete(id);
            return ResponseEntity.ok(ApiResponse.ok(null, "Бафф/дебафф удален"));
        }, controllerTaskExecutor);
    }
}
